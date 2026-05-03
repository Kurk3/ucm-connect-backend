package com.example.ucmconnectapi.dto;

import com.example.ucmconnectapi.entity.File;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO after successful file upload
 * Returns file metadata to the client
 */
public class FileUploadResponse {

    private UUID id;
    private UUID postId;
    private String fileName;
    private File.FileType fileType;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
    private String message;

    // Constructors
    public FileUploadResponse() {
    }

    public FileUploadResponse(UUID id, UUID postId, String fileName, File.FileType fileType,
                              Long fileSize, String mimeType, LocalDateTime createdAt, String message) {
        this.id = id;
        this.postId = postId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
        this.message = message;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
