package com.example.ucmconnectapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache management controller for monitoring and clearing caches
 * Provides endpoints to inspect cache statistics and perform cache operations
 */
@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache Management", description = "Endpoints for cache monitoring and management")
public class CacheController {

    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * Get cache statistics for all caches
     *
     * @return Map of cache names and their statistics
     */
    @Operation(
        summary = "Get cache statistics",
        description = "Returns statistics for all configured caches including hit/miss ratios"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        log.info("Fetching cache statistics");
        Map<String, Object> stats = new HashMap<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("name", cacheName);

                // Get Caffeine cache statistics if available
                if (cache instanceof CaffeineCache) {
                    CaffeineCache caffeineCache = (CaffeineCache) cache;
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                        caffeineCache.getNativeCache();

                    com.github.benmanes.caffeine.cache.stats.CacheStats cacheStatsObj =
                        nativeCache.stats();

                    cacheStats.put("hitCount", cacheStatsObj.hitCount());
                    cacheStats.put("missCount", cacheStatsObj.missCount());
                    cacheStats.put("hitRate", String.format("%.2f%%", cacheStatsObj.hitRate() * 100));
                    cacheStats.put("missRate", String.format("%.2f%%", cacheStatsObj.missRate() * 100));
                    cacheStats.put("evictionCount", cacheStatsObj.evictionCount());
                    cacheStats.put("estimatedSize", nativeCache.estimatedSize());
                } else {
                    cacheStats.put("status", "active");
                }

                stats.put(cacheName, cacheStats);
            }
        });

        return ResponseEntity.ok(stats);
    }

    /**
     * Clear all caches
     *
     * @return Success message
     */
    @Operation(
        summary = "Clear all caches",
        description = "Clears all configured caches in the application"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All caches cleared successfully")
    })
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        log.warn("Clearing all caches");
        int clearedCount = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
                log.info("Cache '{}' cleared", cacheName);
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "All caches cleared successfully");
        response.put("clearedCount", String.valueOf(clearedCount));

        return ResponseEntity.ok(response);
    }

    /**
     * Clear specific cache by name
     *
     * @param cacheName Name of the cache to clear
     * @return Success message
     */
    @Operation(
        summary = "Clear specific cache",
        description = "Clears a specific cache by name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
        @ApiResponse(responseCode = "404", description = "Cache not found")
    })
    @PostMapping("/clear/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        log.warn("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.clear();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cache '" + cacheName + "' cleared successfully");
            log.info("Cache '{}' cleared", cacheName);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Cache '" + cacheName + "' not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Get list of all cache names
     *
     * @return List of cache names
     */
    @Operation(
        summary = "Get cache names",
        description = "Returns list of all configured cache names"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache names retrieved successfully")
    })
    @GetMapping("/names")
    public ResponseEntity<Map<String, Object>> getCacheNames() {
        log.info("Fetching cache names");
        Map<String, Object> response = new HashMap<>();
        response.put("cacheNames", cacheManager.getCacheNames());
        response.put("count", cacheManager.getCacheNames().size());
        return ResponseEntity.ok(response);
    }
}
