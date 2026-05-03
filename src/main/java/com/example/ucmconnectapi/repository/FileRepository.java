package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for File entity
 * Provides data access methods for file management
 */
@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    /**
     * Find all files attached to a specific post
     * @param postId Post UUID
     * @return List of files
     */
    List<File> findByPostIdOrderByCreatedAtDesc(UUID postId);

    /**
     * Find all files uploaded by a specific user
     * @param userId User UUID
     * @return List of files
     */
    List<File> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Count files attached to a specific post
     * @param postId Post UUID
     * @return Number of files
     */
    long countByPostId(UUID postId);

    /**
     * Delete all files associated with a post
     * (This is automatically handled by ON DELETE CASCADE, but explicit method for clarity)
     * @param postId Post UUID
     */
    void deleteByPostId(UUID postId);
}
