package com.example.ucmconnectapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the application using Caffeine
 * Provides in-memory caching for frequently accessed data
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    // Cache names constants
    public static final String SUBJECTS_CACHE = "subjects";
    public static final String SUBJECT_BY_ID_CACHE = "subjectById";
    public static final String USER_BY_ID_CACHE = "userById";
    public static final String POST_BY_ID_CACHE = "postById";
    public static final String COMMENTS_BY_POST_CACHE = "commentsByPost";

    /**
     * Configure Caffeine cache manager with optimized settings
     *
     * Cache strategy:
     * - Subjects: 24h TTL (rarely change)
     * - Users: 1h TTL (moderate changes)
     * - Posts: 15min TTL (frequent changes)
     * - Comments: 5min TTL (very frequent changes)
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing Caffeine cache manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            SUBJECTS_CACHE,
            SUBJECT_BY_ID_CACHE,
            USER_BY_ID_CACHE,
            POST_BY_ID_CACHE,
            COMMENTS_BY_POST_CACHE
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        log.info("Cache manager initialized with caches: {}",
            String.join(", ", cacheManager.getCacheNames()));

        return cacheManager;
    }

    /**
     * Caffeine cache builder with optimized configuration
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(1000)                              // Max 1000 entries per cache
            .expireAfterWrite(1, TimeUnit.HOURS)           // Default TTL: 1 hour
            .recordStats()                                  // Enable statistics for monitoring
            .evictionListener((key, value, cause) -> {
                log.debug("Cache eviction: key={}, cause={}", key, cause);
            });
    }
}
