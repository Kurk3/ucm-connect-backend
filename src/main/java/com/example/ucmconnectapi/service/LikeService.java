package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.dto.LikeDTO;
import com.example.ucmconnectapi.entity.Comment;
import com.example.ucmconnectapi.entity.Like;
import com.example.ucmconnectapi.entity.Post;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.exception.ConflictException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.CommentRepository;
import com.example.ucmconnectapi.repository.LikeRepository;
import com.example.ucmconnectapi.repository.PostRepository;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing likes
 * Contains business logic for like/unlike operations
 */
@Service
public class LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository, PostRepository postRepository,
                       CommentRepository commentRepository, UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Like a post (invalidates post cache)
     *
     * @param postId        Post UUID to like
     * @param currentUserId User performing the like
     * @return Created LikeDTO
     * @throws ResourceNotFoundException if post or user not found
     * @throws ConflictException         if user already liked this post
     */
    @CacheEvict(value = "postById", key = "#postId")
    @Transactional
    public LikeDTO likePost(UUID postId, UUID currentUserId) {
        log.info("User {} liking post {} - will invalidate cache", currentUserId, postId);

        // Check if already liked
        if (likeRepository.existsByUserIdAndPostId(currentUserId, postId)) {
            throw new ConflictException("You have already liked this post");
        }

        // Find post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Find user
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Create like
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        Like savedLike = likeRepository.save(like);

        // Update post's like counter
        post.setNumberOfLikes(post.getNumberOfLikes() + 1);
        postRepository.save(post);

        log.info("Like created successfully with id: {}", savedLike.getId());
        return convertToDTO(savedLike);
    }

    /**
     * Unlike a post (invalidates post cache)
     *
     * @param postId        Post UUID to unlike
     * @param currentUserId User performing the unlike
     * @throws ResourceNotFoundException if post not found or not liked
     */
    @CacheEvict(value = "postById", key = "#postId")
    @Transactional
    public void unlikePost(UUID postId, UUID currentUserId) {
        log.info("User {} unliking post {} - will invalidate cache", currentUserId, postId);

        // Find the like
        Like like = likeRepository.findByUserIdAndPostId(currentUserId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Like", "userId and postId", currentUserId + " and " + postId));

        // Get post to update counter
        Post post = like.getPost();

        // Delete like
        likeRepository.delete(like);

        // Update post's like counter
        post.setNumberOfLikes(Math.max(0, post.getNumberOfLikes() - 1));
        postRepository.save(post);

        log.info("Like removed successfully from post {}", postId);
    }

    /**
     * Like a comment (invalidates comment cache)
     *
     * @param commentId     Comment UUID to like
     * @param currentUserId User performing the like
     * @return Created LikeDTO
     * @throws ResourceNotFoundException if comment or user not found
     * @throws ConflictException         if user already liked this comment
     */
    @CacheEvict(value = "commentsByPost", allEntries = true)
    @Transactional
    public LikeDTO likeComment(UUID commentId, UUID currentUserId) {
        log.info("User {} liking comment {} - will invalidate cache", currentUserId, commentId);

        // Check if already liked
        if (likeRepository.existsByUserIdAndCommentId(currentUserId, commentId)) {
            throw new ConflictException("You have already liked this comment");
        }

        // Find comment
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Find user
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        // Create like
        Like like = new Like();
        like.setUser(user);
        like.setComment(comment);
        Like savedLike = likeRepository.save(like);

        // Update comment's like counter
        comment.setNumberOfLikes(comment.getNumberOfLikes() + 1);
        commentRepository.save(comment);

        log.info("Like created successfully with id: {}", savedLike.getId());
        return convertToDTO(savedLike);
    }

    /**
     * Unlike a comment (invalidates comment cache)
     *
     * @param commentId     Comment UUID to unlike
     * @param currentUserId User performing the unlike
     * @throws ResourceNotFoundException if comment not found or not liked
     */
    @CacheEvict(value = "commentsByPost", allEntries = true)
    @Transactional
    public void unlikeComment(UUID commentId, UUID currentUserId) {
        log.info("User {} unliking comment {} - will invalidate cache", currentUserId, commentId);

        // Find the like
        Like like = likeRepository.findByUserIdAndCommentId(currentUserId, commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Like", "userId and commentId", currentUserId + " and " + commentId));

        // Get comment to update counter
        Comment comment = like.getComment();

        // Delete like
        likeRepository.delete(like);

        // Update comment's like counter
        comment.setNumberOfLikes(Math.max(0, comment.getNumberOfLikes() - 1));
        commentRepository.save(comment);

        log.info("Like removed successfully from comment {}", commentId);
    }

    /**
     * Get all likes by current user
     *
     * @param currentUserId User ID
     * @return List of LikeDTOs
     */
    @Transactional(readOnly = true)
    public List<LikeDTO> getUserLikes(UUID currentUserId) {
        log.info("Fetching all likes for user: {}", currentUserId);

        // Validate user exists
        if (!userRepository.existsById(currentUserId)) {
            throw new ResourceNotFoundException("User", "id", currentUserId);
        }

        List<Like> likes = likeRepository.findByUserId(currentUserId);
        return likes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Like entity to LikeDTO
     */
    private LikeDTO convertToDTO(Like like) {
        LikeDTO dto = new LikeDTO();
        dto.setId(like.getId());
        dto.setUserId(like.getUser().getId());

        if (like.getPost() != null) {
            dto.setPostId(like.getPost().getId());
        }

        if (like.getComment() != null) {
            dto.setCommentId(like.getComment().getId());
        }

        dto.setCreatedAt(like.getCreatedAt());
        return dto;
    }
}
