package com.example.ucmconnectapi.repository;

import com.example.ucmconnectapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 * Provides data access methods for user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by name (username)
     */
    Optional<User> findByName(String name);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists
     */
    boolean existsByName(String name);

    /**
     * Find user by Cognito user ID
     */
    Optional<User> findByCognitoUserId(String cognitoUserId);
}
