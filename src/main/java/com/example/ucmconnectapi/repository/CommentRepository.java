package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Comment entity
 * Provides data access methods for comment management
 *
 * Optimized with JOIN FETCH to avoid N+1 problem:
 * - Without JOIN FETCH: 50 comments = 1 + 50 (users) = 51 SQL queries
 * - With JOIN FETCH: 50 comments = 1 SQL query (single JOIN)
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Find all comments for a specific post — JOIN FETCH user to avoid N+1
     */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    Page<Comment> findByPostIdOrderByCreatedAtDesc(@Param("postId") UUID postId, Pageable pageable);

    /**
     * Find all comments by a specific user — JOIN FETCH user
     */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.user.id = :userId ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    Page<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Count total comments for a specific post
     */
    long countByPostId(UUID postId);
}
