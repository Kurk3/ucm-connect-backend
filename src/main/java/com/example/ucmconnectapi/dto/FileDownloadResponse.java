package com.example.ucmconnectapi.dto;

import java.util.UUID;

/**
 * Response DTO for file download
 * Contains pre-signed URL for downloading from S3
 */
public class FileDownloadResponse {

    private UUID fileId;
    private String fileName;
    private String downloadUrl;
    private String expiresIn;

    // Constructors
    public FileDownloadResponse() {
    }

    public FileDownloadResponse(UUID fileId, String fileName, String downloadUrl, String expiresIn) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String expiresIn) {
        this.expiresIn = expiresIn;
    }
}
