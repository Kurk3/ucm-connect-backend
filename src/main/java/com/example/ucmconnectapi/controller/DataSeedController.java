package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.service.DataSeedService;
import com.example.ucmconnectapi.service.ProductionDataSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for test data seeding
 * Provides endpoints to populate and cleanup test data
 */
@RestController
@RequestMapping("/api/v1/test")
public class DataSeedController {

    private static final Logger logger = LoggerFactory.getLogger(DataSeedController.class);

    @Autowired
    private DataSeedService dataSeedService;

    @Autowired
    private ProductionDataSeedService productionDataSeedService;

    /**
     * Seed test data
     * POST /api/v1/test/seed
     *
     * Creates:
     * - Computer Science subjects
     * - Posts by kurekadam314@gmail.com
     * - Comments on posts
     * - Likes on posts and comments
     *
     * @return JSON summary of created data
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData() {
        logger.info("Received request to seed test data");
        Map<String, Object> result = dataSeedService.seedData();
        return ResponseEntity.ok(result);
    }

    /**
     * Cleanup test data
     * DELETE /api/v1/test/seed
     *
     * Deletes all:
     * - Likes
     * - Comments
     * - Posts
     * - Subjects
     *
     * (Users are preserved for authentication)
     *
     * @return JSON summary of deleted data
     */
    @DeleteMapping("/seed")
    public ResponseEntity<Map<String, Object>> cleanupData() {
        logger.info("Received request to cleanup test data");
        Map<String, Object> result = dataSeedService.cleanupData();
        return ResponseEntity.ok(result);
    }

    /**
     * Seed production data with real study materials
     * POST /api/v1/test/seed-production
     */
    @PostMapping("/seed-production")
    public ResponseEntity<Map<String, Object>> seedProductionData() {
        logger.info("Received request to seed production data");
        Map<String, Object> result = productionDataSeedService.seedProductionData();
        return ResponseEntity.ok(result);
    }

    /**
     * Drop all production data (keeps subjects)
     * DELETE /api/v1/test/seed-production
     */
    @DeleteMapping("/seed-production")
    public ResponseEntity<Map<String, Object>> dropProductionData() {
        logger.warn("Received request to drop production data");
        Map<String, Object> result = productionDataSeedService.dropProductionData();
        return ResponseEntity.ok(result);
    }

    /**
     * Get seeding status
     * GET /api/v1/test/seed
     *
     * @return Current database counts
     */
    @GetMapping("/seed")
    public ResponseEntity<Map<String, Object>> getSeedStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "ready",
            "message", "Use POST to seed data, DELETE to cleanup",
            "endpoints", Map.of(
                "seed", "POST /api/v1/test/seed",
                "seed-production", "POST /api/v1/test/seed-production",
                "cleanup", "DELETE /api/v1/test/seed",
                "status", "GET /api/v1/test/seed"
            )
        ));
    }
}
