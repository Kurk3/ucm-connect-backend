package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Subject entity
 * Provides database operations for subjects/categories
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    /**
     * Find subject by name (case-sensitive)
     * Used to prevent duplicate subject names
     *
     * @param name The subject name to search for
     * @return Optional containing the subject if found
     */
    Optional<Subject> findByName(String name);

    /**
     * Check if subject with given name exists
     *
     * @param name The subject name to check
     * @return true if subject exists, false otherwise
     */
    boolean existsByName(String name);
}
