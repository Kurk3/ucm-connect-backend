package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Like entity
 * Provides data access methods for like management
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    /**
     * Find like by user and post
     */
    Optional<Like> findByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Find like by user and comment
     */
    Optional<Like> findByUserIdAndCommentId(UUID userId, UUID commentId);

    /**
     * Check if user already liked a post
     */
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Check if user already liked a comment
     */
    boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

    /**
     * Get all likes by a specific user
     */
    List<Like> findByUserId(UUID userId);

    /**
     * Count total likes for a specific post
     */
    long countByPostId(UUID postId);

    /**
     * Count total likes for a specific comment
     */
    long countByCommentId(UUID commentId);
}
