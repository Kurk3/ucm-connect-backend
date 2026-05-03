package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.dto.FileDTO;
import com.example.ucmconnectapi.dto.FileDownloadResponse;
import com.example.ucmconnectapi.dto.FileUploadResponse;
import com.example.ucmconnectapi.entity.File;
import com.example.ucmconnectapi.service.FileService;
import com.example.ucmconnectapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for file management
 * Handles file upload, download, and deletion
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Files", description = "File management endpoints for post attachments")
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private UserService userService;

    /**
     * Upload file to a post
     * POST /api/v1/posts/{postId}/files
     */
    @Operation(
            summary = "Upload file to a post",
            description = "Upload a file attachment (PDF, image, ZIP) to a post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or file too large",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/posts/{postId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable UUID postId,
            @RequestParam("file") MultipartFile file) {

        UUID currentUserId = userService.getCurrentUserIdFromToken();
        logger.info("User {} uploading file to post {}", currentUserId, postId);

        File uploadedFile = fileService.uploadFile(postId, file, currentUserId);

        FileUploadResponse response = new FileUploadResponse(
                uploadedFile.getId(),
                uploadedFile.getPost().getId(),
                uploadedFile.getFileName(),
                uploadedFile.getFileType(),
                uploadedFile.getFileSize(),
                uploadedFile.getMimeType(),
                uploadedFile.getCreatedAt(),
                "File uploaded successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all files for a post
     * GET /api/v1/posts/{postId}/files
     */
    @Operation(
            summary = "Get all files for a post",
            description = "Retrieve list of all file attachments for a specific post. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/posts/{postId}/files")
    public ResponseEntity<List<FileDTO>> getPostFiles(@PathVariable UUID postId) {
        logger.info("Fetching files for post {}", postId);

        List<File> files = fileService.getPostFiles(postId);
        List<FileDTO> fileDTOs = files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileDTOs);
    }

    /**
     * Get file metadata by ID
     * GET /api/v1/files/{fileId}
     */
    @Operation(
            summary = "Get file metadata",
            description = "Retrieve metadata for a specific file by ID. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FileDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{fileId}")
    public ResponseEntity<FileDTO> getFile(@PathVariable UUID fileId) {
        logger.info("Fetching file metadata for {}", fileId);

        File file = fileService.getFileById(fileId);
        FileDTO fileDTO = convertToDTO(file);

        return ResponseEntity.ok(fileDTO);
    }

    /**
     * Get download URL for file
     * GET /api/v1/files/{fileId}/download
     * Returns pre-signed S3 URL (valid for 1 hour)
     */
    @Operation(
            summary = "Get file download URL",
            description = "Generate a pre-signed S3 URL for downloading a file. URL is valid for 1 hour. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download URL generated successfully",
                    content = @Content(schema = @Schema(implementation = FileDownloadResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<FileDownloadResponse> getDownloadUrl(@PathVariable UUID fileId) {
        logger.info("Generating download URL for file {}", fileId);

        File file = fileService.getFileById(fileId);
        String downloadUrl = fileService.getDownloadUrl(fileId);

        FileDownloadResponse response = new FileDownloadResponse(
                file.getId(),
                file.getFileName(),
                downloadUrl,
                "1 hour"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Delete file
     * DELETE /api/v1/files/{fileId}
     * Only file owner or post owner can delete
     */
    @Operation(
            summary = "Delete file",
            description = "Delete a file attachment. Only the file owner or post owner can delete. Requires valid JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have permission to delete this file",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        logger.info("User {} deleting file {}", currentUserId, fileId);

        fileService.deleteFile(fileId, currentUserId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get file count for a post
     * GET /api/v1/posts/{postId}/files/count
     */
    @GetMapping("/posts/{postId}/files/count")
    public ResponseEntity<Long> getFileCount(@PathVariable UUID postId) {
        logger.debug("Counting files for post {}", postId);

        long count = fileService.countPostFiles(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * Convert File entity to FileDTO
     * Generates pre-signed S3 URL for file download
     */
    private FileDTO convertToDTO(File file) {
        FileDTO dto = new FileDTO();
        dto.setId(file.getId());
        dto.setPostId(file.getPost().getId());
        dto.setUserId(file.getUser().getId());
        dto.setUserName(file.getUser().getName());
        dto.setFileName(file.getFileName());
        dto.setFileType(file.getFileType());
        dto.setFileSize(file.getFileSize());
        dto.setMimeType(file.getMimeType());
        dto.setCreatedAt(file.getCreatedAt());

        // Generate pre-signed S3 URL (valid for 1 hour)
        String downloadUrl = fileService.getDownloadUrl(file.getId());
        dto.setUrl(downloadUrl);

        return dto;
    }
}
