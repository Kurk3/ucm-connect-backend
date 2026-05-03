package com.example.ucmconnectapi.dto;

import com.example.ucmconnectapi.entity.File;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for File entity
 * Used for returning file information in API responses
 */
public class FileDTO {

    private UUID id;
    private UUID postId;
    private UUID userId;
    private String userName;
    private String fileName;
    private File.FileType fileType;
    private Long fileSize;
    private String mimeType;

    @Schema(description = "Pre-signed S3 URL for downloading file (valid for 1 hour)",
            example = "https://ucm-connect-files-2025.s3.eu-central-1.amazonaws.com/...")
    private String url;

    private LocalDateTime createdAt;

    // Constructors
    public FileDTO() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public File.FileType getFileType() {
        return fileType;
    }

    public void setFileType(File.FileType fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
