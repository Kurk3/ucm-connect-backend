package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.entity.File;
import com.example.ucmconnectapi.entity.Post;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.FileRepository;
import com.example.ucmconnectapi.repository.PostRepository;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * High-level file management service
 * Handles business logic for file operations (upload, download, delete)
 */
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3FileStorageService s3FileStorageService;

    /**
     * Upload file to a post
     * @param postId Post UUID
     * @param uploadedFile Multipart file
     * @param currentUserId User uploading the file
     * @return Created File entity
     */
    @Transactional
    public File uploadFile(UUID postId, MultipartFile uploadedFile, UUID currentUserId) {
        logger.info("Uploading file to post: {} by user: {}", postId, currentUserId);

        // Verify post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + postId));

        // Verify user exists
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        String s3Key = null;
        try {
            // Upload to S3
            s3Key = s3FileStorageService.uploadFile(uploadedFile);

            // Create File entity
            File file = new File();
            file.setPost(post);
            file.setUser(user);
            file.setFileName(uploadedFile.getOriginalFilename());
            file.setFileKey(s3Key);
            file.setFileSize(uploadedFile.getSize());
            file.setMimeType(uploadedFile.getContentType());
            file.setFileType(s3FileStorageService.determineFileType(uploadedFile.getContentType()));

            File savedFile = fileRepository.save(file);
            logger.info("File saved to database: {} (S3 key: {})", savedFile.getId(), s3Key);
            return savedFile;

        } catch (Exception e) {
            // Cleanup: Delete from S3 if database save failed
            if (s3Key != null) {
                logger.error("Database save failed, cleaning up S3 file: {}", s3Key);
                try {
                    s3FileStorageService.deleteFile(s3Key);
                } catch (Exception cleanupError) {
                    logger.error("Failed to cleanup S3 file after error: {}", s3Key, cleanupError);
                }
            }
            throw e; // Re-throw original exception
        }
    }

    /**
     * Get all files for a post
     * @param postId Post UUID
     * @return List of files
     */
    @Transactional(readOnly = true)
    public List<File> getPostFiles(UUID postId) {
        logger.debug("Fetching files for post: {}", postId);

        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with ID: " + postId);
        }

        return fileRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    /**
     * Get file by ID
     * @param fileId File UUID
     * @return File entity
     */
    @Transactional(readOnly = true)
    public File getFileById(UUID fileId) {
        logger.debug("Fetching file: {}", fileId);
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with ID: " + fileId));
    }

    /**
     * Generate download URL for file
     * @param fileId File UUID
     * @return Pre-signed S3 URL
     */
    public String getDownloadUrl(UUID fileId) {
        logger.info("Generating download URL for file: {}", fileId);

        File file = getFileById(fileId);
        return s3FileStorageService.generatePresignedUrl(file.getFileKey());
    }

    /**
     * Delete file
     * Owner (file or post), Admin, or Moderator can delete
     *
     * @param fileId File UUID
     * @param currentUserId User requesting deletion
     */
    @Transactional
    public void deleteFile(UUID fileId, UUID currentUserId) {
        logger.info("Deleting file: {} by user: {}", fileId, currentUserId);

        File file = getFileById(fileId);

        // Find current user to check role
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        // Check permission: file owner OR post owner OR admin/moderator
        boolean isFileOwner = file.getUser().getId().equals(currentUserId);
        boolean isPostOwner = file.getPost().getUser().getId().equals(currentUserId);
        boolean canModerate = currentUser.isAdminOrModerator();

        if (!isFileOwner && !isPostOwner && !canModerate) {
            logger.warn("User {} attempted to delete file {} without permission", currentUserId, fileId);
            throw new ForbiddenException("You don't have permission to delete this file");
        }

        // Delete from S3
        s3FileStorageService.deleteFile(file.getFileKey());

        // Delete from database
        fileRepository.delete(file);
        logger.info("File deleted: {} by user: {} (role: {})", fileId, currentUserId, currentUser.getRole());
    }

    /**
     * Count files for a post
     * @param postId Post UUID
     * @return Number of files
     */
    public long countPostFiles(UUID postId) {
        return fileRepository.countByPostId(postId);
    }

    /**
     * Get test user ID by email (for localhost testing without authentication)
     * TODO: Remove this method in production
     *
     * @param email Test user email
     * @return User UUID
     * @throws ResourceNotFoundException if test user not found
     */
    public UUID getTestUserId(String email) {
        User testUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Test user not found with email: " + email +
                    ". Please create a test user first or use authentication."));
        return testUser.getId();
    }
}
