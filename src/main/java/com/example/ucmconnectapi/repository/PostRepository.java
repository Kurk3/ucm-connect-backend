package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Post entity
 * Provides data access methods for posts with filtering and pagination support
 *
 * Optimized with JOIN FETCH to avoid N+1 problem:
 * - Without JOIN FETCH: 20 posts = 1 + 20 (users) + 20 (subjects) = 41 SQL queries
 * - With JOIN FETCH: 20 posts = 1 SQL query (single JOIN)
 */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Find all posts ordered by creation date (newest first)
     */
    List<Post> findAllByOrderByCreatedAtDesc();

    /**
     * Find all posts with pagination — JOIN FETCH user and subject to avoid N+1
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.subject ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find posts by subject ID
     */
    List<Post> findBySubjectId(UUID subjectId);

    /**
     * Find posts by subject ID with pagination — JOIN FETCH user and subject
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.subject WHERE p.subject.id = :subjectId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.subject.id = :subjectId")
    Page<Post> findBySubjectIdOrderByCreatedAtDesc(@Param("subjectId") UUID subjectId, Pageable pageable);

    /**
     * Find posts by user ID
     */
    List<Post> findByUserId(UUID userId);

    /**
     * Find posts by user ID with pagination — JOIN FETCH user and subject
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.subject WHERE p.user.id = :userId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
    Page<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find posts by both subject and user — JOIN FETCH user and subject
     */
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.subject WHERE p.subject.id = :subjectId AND p.user.id = :userId ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.subject.id = :subjectId AND p.user.id = :userId")
    Page<Post> findBySubjectIdAndUserIdOrderByCreatedAtDesc(@Param("subjectId") UUID subjectId, @Param("userId") UUID userId, Pageable pageable);

    /**
     * Find post by ID with user and subject pre-loaded — for detail view
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.subject WHERE p.id = :id")
    Optional<Post> findByIdWithUserAndSubject(@Param("id") UUID id);
}
