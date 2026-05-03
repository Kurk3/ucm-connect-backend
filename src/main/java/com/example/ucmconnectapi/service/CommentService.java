package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.dto.CommentDTO;
import com.example.ucmconnectapi.dto.CommentDetailDTO;
import com.example.ucmconnectapi.dto.CommentListResponse;
import com.example.ucmconnectapi.dto.nested.AuthorDTO;
import com.example.ucmconnectapi.dto.nested.StatsDTO;
import com.example.ucmconnectapi.entity.Comment;
import com.example.ucmconnectapi.entity.Post;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.util.HtmlSanitizer;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.CommentRepository;
import com.example.ucmconnectapi.repository.PostRepository;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing comments
 * Contains business logic for comment operations
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all comments for a specific post with pagination (cached for 5 minutes)
     *
     * @param postId Post UUID
     * @param page   Page number (1-indexed)
     * @param limit  Items per page (max 100)
     * @return CommentListResponse with paginated comments
     */
    @Cacheable(value = "commentsByPost", key = "#postId + '-' + #page + '-' + #limit")
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByPostId(UUID postId, int page, int limit) {
        log.info("⚠️ CACHE MISS - Fetching comments for post: {}, page={}, limit={}", postId, page, limit);

        // Validate post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        // Validate and normalize pagination parameters
        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);  // Spring uses 0-indexed pages
        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Found {} comments for post {} (total: {})", commentDTOs.size(), postId, commentPage.getTotalElements());
        return new CommentListResponse(commentDTOs, (int) commentPage.getTotalElements(), page, limit);
    }

    /**
     * Get single comment by ID (flat structure for backwards compatibility)
     *
     * @param id Comment UUID
     * @return CommentDTO
     * @throws ResourceNotFoundException if comment not found
     */
    @Transactional(readOnly = true)
    public CommentDTO getCommentById(UUID id) {
        log.info("Fetching comment with id: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        return convertToDTO(comment);
    }

    /**
     * Get comment by ID with detailed nested structure
     *
     * @param id Comment UUID
     * @return CommentDetailDTO with nested objects (author, stats)
     * @throws ResourceNotFoundException if comment not found
     */
    @Transactional(readOnly = true)
    public CommentDetailDTO getCommentDetailById(UUID id) {
        log.info("Fetching detailed comment with id: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        return convertToDetailDTO(comment);
    }

    /**
     * Create new comment on a post (invalidates comment cache and post cache)
     *
     * @param postId        Post UUID to comment on
     * @param commentDTO    Comment data
     * @param currentUserId User creating the comment
     * @return Created CommentDTO
     * @throws ResourceNotFoundException if post or user not found
     */
    @Caching(evict = {
        @CacheEvict(value = "commentsByPost", allEntries = true),
        @CacheEvict(value = "postById", key = "#postId")
    })
    @Transactional
    public CommentDTO createComment(UUID postId, CommentDTO commentDTO, UUID currentUserId) {
        log.info("Creating new comment on post {} by user: {} - will invalidate cache", postId, currentUserId);

        // Find post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Find user
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Create new comment entity
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(HtmlSanitizer.sanitize(commentDTO.getContent().trim()));
        comment.setNumberOfLikes(0);

        // Save to database
        Comment savedComment = commentRepository.save(comment);

        // Update post's comment counter
        post.setNumberOfComments(post.getNumberOfComments() + 1);
        postRepository.save(post);

        log.info("Comment created successfully with id: {}", savedComment.getId());
        return convertToDTO(savedComment);
    }

    /**
     * Update existing comment (invalidates comment cache)
     *
     * @param id            Comment UUID
     * @param commentDTO    Updated comment data
     * @param currentUserId User performing the update
     * @return Updated CommentDTO
     * @throws ResourceNotFoundException if comment not found
     * @throws ForbiddenException        if user is not the comment owner
     */
    @CacheEvict(value = "commentsByPost", allEntries = true)
    @Transactional
    public CommentDTO updateComment(UUID id, CommentDTO commentDTO, UUID currentUserId) {
        log.info("Updating comment with id: {} by user: {} - will invalidate cache", id, currentUserId);

        // Find comment
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        // Check ownership
        if (!comment.getUser().getId().equals(currentUserId)) {
            log.warn("User {} attempted to update comment {} owned by {}", currentUserId, id, comment.getUser().getId());
            throw new ForbiddenException("You are not the owner of this comment");
        }

        // Update content
        comment.setContent(HtmlSanitizer.sanitize(commentDTO.getContent().trim()));

        // Save changes
        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment updated successfully: {}", updatedComment.getId());

        return convertToDTO(updatedComment);
    }

    /**
     * Delete comment (invalidates comment cache and post cache)
     * Owner, Admin, or Moderator can delete
     *
     * @param id            Comment UUID
     * @param currentUserId User performing the deletion
     * @throws ResourceNotFoundException if comment not found
     * @throws ForbiddenException        if user doesn't have permission
     */
    @Transactional
    public void deleteComment(UUID id, UUID currentUserId) {
        log.info("Deleting comment with id: {} by user: {} - will invalidate cache", id, currentUserId);

        // Find comment
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));

        // Find current user to check role
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Check permission: owner OR admin/moderator
        boolean isOwner = comment.getUser().getId().equals(currentUserId);
        boolean canModerate = currentUser.isAdminOrModerator();

        if (!isOwner && !canModerate) {
            log.warn("User {} attempted to delete comment {} without permission", currentUserId, id);
            throw new ForbiddenException("You don't have permission to delete this comment");
        }

        // Get post to update counter
        Post post = comment.getPost();
        UUID postId = post.getId();

        // Delete comment
        commentRepository.deleteById(id);

        // Update post's comment counter
        post.setNumberOfComments(Math.max(0, post.getNumberOfComments() - 1));
        postRepository.save(post);

        // Manually invalidate caches after deletion
        evictCommentAndPostCache(postId);

        log.info("Comment deleted successfully: {} by user: {} (role: {})", id, currentUserId, currentUser.getRole());
    }

    /**
     * Helper method to evict comment and post caches
     */
    @Caching(evict = {
        @CacheEvict(value = "commentsByPost", allEntries = true),
        @CacheEvict(value = "postById", key = "#postId")
    })
    public void evictCommentAndPostCache(UUID postId) {
        log.debug("Evicting cache for post: {}", postId);
    }

    /**
     * Convert Comment entity to CommentDTO (flat structure for lists)
     */
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        dto.setUserId(comment.getUser().getId());
        dto.setUserName(comment.getUser().getName());
        dto.setContent(comment.getContent());
        dto.setNumberOfLikes(comment.getNumberOfLikes());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }

    /**
     * Convert Comment entity to CommentDetailDTO (nested structure for detail view)
     */
    private CommentDetailDTO convertToDetailDTO(Comment comment) {
        // Build author nested object
        AuthorDTO author = AuthorDTO.builder()
                .id(comment.getUser().getId())
                .name(comment.getUser().getName())
                .email(comment.getUser().getEmail())
                .build();

        // Build stats nested object
        StatsDTO stats = StatsDTO.builder()
                .likes(comment.getNumberOfLikes())
                .comments(0)  // Comments don't have nested comments
                .views(0)     // Placeholder for future views feature
                .build();

        // Build and return detail DTO
        return CommentDetailDTO.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .author(author)
                .content(comment.getContent())
                .stats(stats)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
