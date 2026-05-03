package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.entity.File;
import jakarta.annotation.PostConstruct;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Low-level S3 storage service
 * Handles direct AWS S3 operations: upload, download URL generation, and delete
 */
@Service
public class S3FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageService.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access.key.id}")
    private String accessKeyId;

    @Value("${aws.secret.access.key}")
    private String secretAccessKey;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    // Allowed file types
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "png", "jpg", "jpeg", "zip");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/zip",
            "application/x-zip-compressed"
    );

    // Max file size: 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // Pre-signed URL expiration
    private static final Duration PRESIGNED_URL_DURATION = Duration.ofHours(1);

    @PostConstruct
    public void init() {
        Region awsRegion = Region.of(region);
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        logger.info("S3 client initialized for bucket: {} in region: {}", bucketName, region);
    }

    /**
     * Upload file to S3
     * @param file Multipart file
     * @return S3 object key (unique identifier)
     */
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileKey = UUID.randomUUID() + "_" + sanitizeFilename(originalFilename);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            logger.info("File uploaded to S3: {} (original: {})", fileKey, originalFilename);
            return fileKey;

        } catch (S3Exception e) {
            logger.error("S3 upload failed: {}", originalFilename, e);
            throw new RuntimeException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage());
        } catch (IOException e) {
            logger.error("Failed to read file: {}", originalFilename, e);
            throw new RuntimeException("Failed to read file: " + originalFilename);
        }
    }

    /**
     * Generate pre-signed URL for download
     * @param fileKey S3 object key
     * @return Pre-signed URL (valid for 1 hour)
     */
    public String generatePresignedUrl(String fileKey) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_DURATION)
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            logger.info("Generated pre-signed URL for: {}", fileKey);
            return presigned.url().toString();

        } catch (S3Exception e) {
            logger.error("Failed to generate pre-signed URL: {}", fileKey, e);
            throw new RuntimeException("File not found in S3: " + fileKey);
        }
    }

    /**
     * Delete file from S3
     * @param fileKey S3 object key
     */
    public void deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("File deleted from S3: {}", fileKey);

        } catch (S3Exception e) {
            logger.error("Failed to delete from S3: {}", fileKey, e);
            throw new RuntimeException("Failed to delete file from S3");
        }
    }

    /**
     * Delete ALL files from S3 bucket
     * WARNING: This is a destructive operation! Use only for testing/cleanup.
     * @return Number of files deleted
     */
    public int deleteAllFilesInBucket() {
        logger.warn("DELETING ALL FILES FROM S3 BUCKET: {}", bucketName);
        int deletedCount = 0;

        try {
            // List all objects in bucket
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse;
            do {
                listResponse = s3Client.listObjectsV2(listRequest);
                List<S3Object> objects = listResponse.contents();

                if (objects.isEmpty()) {
                    logger.info("No files found in S3 bucket");
                    break;
                }

                // Delete each object
                for (S3Object object : objects) {
                    try {
                        deleteFile(object.key());
                        deletedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to delete S3 object: {}", object.key(), e);
                        // Continue deleting other files even if one fails
                    }
                }

                // Check if there are more objects (pagination)
                listRequest = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .continuationToken(listResponse.nextContinuationToken())
                        .build();

            } while (listResponse.isTruncated());

            logger.warn("Deleted {} files from S3 bucket: {}", deletedCount, bucketName);
            return deletedCount;

        } catch (S3Exception e) {
            logger.error("Failed to list/delete objects from S3 bucket", e);
            throw new RuntimeException("Failed to cleanup S3 bucket: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Determine file type from MIME type
     */
    public File.FileType determineFileType(String mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("MIME type is required");
        }

        String type = mimeType.toLowerCase();
        if (type.equals("application/pdf")) {
            return File.FileType.PDF;
        } else if (type.startsWith("image/")) {
            return File.FileType.IMAGE;
        } else if (type.contains("zip")) {
            return File.FileType.ZIP;
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File must have a name");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds %d MB limit", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file MIME type");
        }

        // Validate actual file content using magic bytes (Apache Tika)
        try {
            Tika tika = new Tika();
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(detectedType.toLowerCase())) {
                throw new IllegalArgumentException(
                        "File content does not match declared type. Detected: " + detectedType
                );
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to verify file content");
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
