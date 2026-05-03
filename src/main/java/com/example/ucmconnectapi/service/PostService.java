package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.dto.PostDTO;
import com.example.ucmconnectapi.dto.PostDetailDTO;
import com.example.ucmconnectapi.dto.PostListResponse;
import com.example.ucmconnectapi.dto.nested.AuthorDTO;
import com.example.ucmconnectapi.dto.nested.FileInfoDTO;
import com.example.ucmconnectapi.dto.nested.StatsDTO;
import com.example.ucmconnectapi.dto.nested.SubjectInfoDTO;
import com.example.ucmconnectapi.entity.File;
import com.example.ucmconnectapi.entity.Post;
import com.example.ucmconnectapi.entity.Subject;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.util.HtmlSanitizer;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.FileRepository;
import com.example.ucmconnectapi.repository.PostRepository;
import com.example.ucmconnectapi.repository.SubjectRepository;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing posts
 * Contains business logic for post operations
 */
@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final FileRepository fileRepository;

    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository,
                      SubjectRepository subjectRepository, FileRepository fileRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Get all posts with pagination and optional filters
     *
     * @param page      Page number (1-indexed)
     * @param limit     Items per page (max 100)
     * @param subjectId Optional filter by subject
     * @param userId    Optional filter by user
     * @return PostListResponse with paginated posts
     */
    @Transactional(readOnly = true)
    public PostListResponse getAllPosts(int page, int limit, UUID subjectId, UUID userId) {
        log.info("Fetching posts: page={}, limit={}, subjectId={}, userId={}", page, limit, subjectId, userId);

        // Validate and normalize pagination parameters
        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);  // Spring uses 0-indexed pages
        Page<Post> postPage;

        // Apply filters based on parameters
        if (subjectId != null && userId != null) {
            postPage = postRepository.findBySubjectIdAndUserIdOrderByCreatedAtDesc(subjectId, userId, pageable);
        } else if (subjectId != null) {
            postPage = postRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId, pageable);
        } else if (userId != null) {
            postPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<PostDTO> postDTOs = postPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Found {} posts (total: {})", postDTOs.size(), postPage.getTotalElements());
        return new PostListResponse(postDTOs, (int) postPage.getTotalElements(), page, limit);
    }

    /**
     * Get post by ID (flat structure for backwards compatibility)
     *
     * @param id Post UUID
     * @return PostDTO
     * @throws ResourceNotFoundException if post not found
     */
    @Transactional(readOnly = true)
    public PostDTO getPostById(UUID id) {
        log.info("Fetching post with id: {}", id);
        Post post = postRepository.findByIdWithUserAndSubject(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        return convertToDTO(post);
    }

    /**
     * Get post by ID with detailed nested structure
     *
     * @param id Post UUID
     * @return PostDetailDTO with nested objects (author, subject, file, stats)
     * @throws ResourceNotFoundException if post not found
     */
    @Transactional(readOnly = true)
    public PostDetailDTO getPostDetailById(UUID id) {
        log.info("Fetching detailed post with id: {}", id);
        Post post = postRepository.findByIdWithUserAndSubject(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        return convertToDetailDTO(post);
    }

    /**
     * Create new post (with mock file values)
     *
     * @param postDTO       Post data
     * @param currentUserId User creating the post
     * @return Created PostDTO
     * @throws ResourceNotFoundException if user or subject not found
     */
    @Transactional
    public PostDTO createPost(PostDTO postDTO, UUID currentUserId) {
        log.info("Creating new post with title: '{}' by user: {}", postDTO.getTitle(), currentUserId);

        // Find user
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Create new post entity
        Post post = new Post();
        post.setTitle(HtmlSanitizer.sanitize(postDTO.getTitle().trim()));
        post.setDescription(postDTO.getDescription() != null ?
                HtmlSanitizer.sanitize(postDTO.getDescription().trim()) : null);
        post.setUser(user);

        // Handle optional subject
        if (postDTO.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(postDTO.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", postDTO.getSubjectId()));
            post.setSubject(subject);
        } else {
            post.setSubject(null);  // Subject is optional
            log.info("Creating post without subject");
        }

        // Set default values
        post.setNumberOfLikes(0);
        post.setNumberOfComments(0);
        post.setIsPublished(true);

        // Save to database
        Post savedPost = postRepository.save(post);
        log.info("Post created successfully with id: {}", savedPost.getId());

        return convertToDTO(savedPost);
    }

    /**
     * Update existing post
     *
     * @param id            Post UUID
     * @param postDTO       Updated post data
     * @param currentUserId User performing the update
     * @return Updated PostDTO
     * @throws ResourceNotFoundException if post or subject not found
     * @throws ForbiddenException        if user is not the post owner
     */
    @Transactional
    public PostDTO updatePost(UUID id, PostDTO postDTO, UUID currentUserId) {
        log.info("Updating post with id: {} by user: {}", id, currentUserId);

        // Find post
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        // Check ownership
        if (!post.getUser().getId().equals(currentUserId)) {
            log.warn("User {} attempted to update post {} owned by {}", currentUserId, id, post.getUser().getId());
            throw new ForbiddenException("You are not the owner of this post");
        }

        // Update basic fields
        post.setTitle(HtmlSanitizer.sanitize(postDTO.getTitle().trim()));
        post.setDescription(postDTO.getDescription() != null ?
                HtmlSanitizer.sanitize(postDTO.getDescription().trim()) : null);

        // Handle subject change
        if (postDTO.getSubjectId() != null) {
            // Check if subject is changing
            if (post.getSubject() == null || !post.getSubject().getId().equals(postDTO.getSubjectId())) {
                Subject subject = subjectRepository.findById(postDTO.getSubjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", postDTO.getSubjectId()));
                post.setSubject(subject);
            }
        } else {
            post.setSubject(null);  // Removing subject
        }

        // NOTE: File fields are NOT updated here (file update will be separate endpoint)

        // Save changes
        Post updatedPost = postRepository.save(post);
        log.info("Post updated successfully: {}", updatedPost.getId());

        return convertToDTO(updatedPost);
    }

    /**
     * Delete post
     * Owner, Admin, or Moderator can delete
     *
     * @param id            Post UUID
     * @param currentUserId User performing the deletion
     * @throws ResourceNotFoundException if post not found
     * @throws ForbiddenException        if user doesn't have permission
     */
    @Transactional
    public void deletePost(UUID id, UUID currentUserId) {
        log.info("Deleting post with id: {} by user: {}", id, currentUserId);

        // Find post
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));

        // Find current user to check role
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Check permission: owner OR admin/moderator
        boolean isOwner = post.getUser().getId().equals(currentUserId);
        boolean canModerate = currentUser.isAdminOrModerator();

        if (!isOwner && !canModerate) {
            log.warn("User {} attempted to delete post {} without permission", currentUserId, id);
            throw new ForbiddenException("You don't have permission to delete this post");
        }

        // Delete post
        postRepository.deleteById(id);
        log.info("Post deleted successfully: {} by user: {} (role: {})", id, currentUserId, currentUser.getRole());
    }

    /**
     * Convert Post entity to PostDTO (flat structure for lists)
     */
    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());

        // User information
        dto.setUserId(post.getUser().getId());
        dto.setUserName(post.getUser().getName());

        // Subject information (can be null)
        if (post.getSubject() != null) {
            dto.setSubjectId(post.getSubject().getId());
            dto.setSubjectName(post.getSubject().getName());
        }

        // Counters
        dto.setNumberOfLikes(post.getNumberOfLikes());
        dto.setNumberOfComments(post.getNumberOfComments());

        // Status
        dto.setIsPublished(post.getIsPublished());

        // Timestamps
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        return dto;
    }

    /**
     * Convert Post entity to PostDetailDTO (nested structure for detail view)
     */
    private PostDetailDTO convertToDetailDTO(Post post) {
        // Build author nested object
        AuthorDTO author = AuthorDTO.builder()
                .id(post.getUser().getId())
                .name(post.getUser().getName())
                .email(post.getUser().getEmail())
                .build();

        // Build subject nested object (can be null)
        SubjectInfoDTO subject = null;
        if (post.getSubject() != null) {
            subject = SubjectInfoDTO.builder()
                    .id(post.getSubject().getId())
                    .name(post.getSubject().getName())
                    .description(post.getSubject().getDescription())
                    .build();
        }

        // Build file nested object (get first/primary file if exists)
        FileInfoDTO file = null;
        List<File> files = fileRepository.findByPostIdOrderByCreatedAtDesc(post.getId());
        if (!files.isEmpty()) {
            File primaryFile = files.get(0);  // Get most recent file
            file = FileInfoDTO.builder()
                    .type(primaryFile.getFileType().name().toLowerCase())
                    .url(primaryFile.getFileKey())
                    .size(primaryFile.getFileSize())
                    .downloadUrl(null)  // TODO: Generate pre-signed S3 URL in future
                    .build();
        }

        // Build stats nested object
        StatsDTO stats = StatsDTO.builder()
                .likes(post.getNumberOfLikes())
                .comments(post.getNumberOfComments())
                .views(0)  // Placeholder for future views feature
                .build();

        // Build and return detail DTO
        return PostDetailDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .description(post.getDescription())
                .author(author)
                .subject(subject)
                .file(file)
                .stats(stats)
                .isPublished(post.getIsPublished())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
