package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.entity.*;
import com.example.ucmconnectapi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for seeding test data
 * Creates CS-focused test data on demand
 */
@Service
public class DataSeedService {

    private static final Logger logger = LoggerFactory.getLogger(DataSeedService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CognitoService cognitoService;

    @Autowired
    private S3FileStorageService s3FileStorageService;

    @Autowired
    private FileRepository fileRepository;

    private static final String MAIN_USER_EMAIL = "kurekadam314@gmail.com";
    private static final String FALLBACK_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_PASSWORD = "Password123!";

    /**
     * Seed all test data
     * @return Map with counts of created entities
     */
    @Transactional
    public Map<String, Object> seedData() {
        logger.info("Starting data seeding...");

        Map<String, Integer> counts = new HashMap<>();

        // 1. Ensure users exist
        User mainUser = ensureUserExists(MAIN_USER_EMAIL, "Adam Kurek");
        User fallbackUser = ensureUserExists(FALLBACK_USER_EMAIL, "Test User");
        counts.put("users", 2);

        // 2. Create CS subjects
        List<Subject> subjects = createSubjects();
        counts.put("subjects", subjects.size());

        // 3. Create posts
        List<Post> posts = createPosts(mainUser, subjects);
        counts.put("posts", posts.size());

        // 4. Create comments
        List<Comment> comments = createComments(mainUser, fallbackUser, posts);
        counts.put("comments", comments.size());

        // 5. Create likes
        int likesCount = createLikes(mainUser, fallbackUser, posts, comments);
        counts.put("likes", likesCount);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("created", counts);
        result.put("message", "Test data seeded successfully for " + MAIN_USER_EMAIL);

        logger.info("Data seeding completed: {}", counts);
        return result;
    }

    /**
     * Delete ALL data from EVERYWHERE
     * WARNING: This is a DESTRUCTIVE operation!
     * Deletes from: S3 bucket, Cognito user pool, and PostgreSQL database
     *
     * @return Map with counts of deleted entities
     */
    @Transactional
    public Map<String, Object> cleanupData() {
        logger.warn("⚠️  STARTING COMPLETE DATA WIPEOUT - DELETING EVERYTHING!");

        Map<String, Integer> counts = new HashMap<>();

        // 1. Delete ALL files from S3 bucket
        logger.warn("Step 1/3: Deleting all files from S3 bucket...");
        try {
            int s3FilesDeleted = s3FileStorageService.deleteAllFilesInBucket();
            counts.put("s3_files", s3FilesDeleted);
            logger.info("✓ Deleted {} files from S3", s3FilesDeleted);
        } catch (Exception e) {
            logger.error("Failed to delete S3 files (continuing anyway): {}", e.getMessage());
            counts.put("s3_files", 0);
        }

        // 2. Delete ALL users from Cognito user pool
        logger.warn("Step 2/3: Deleting all users from Cognito...");
        try {
            int cognitoUsersDeleted = cognitoService.deleteAllUsers();
            counts.put("cognito_users", cognitoUsersDeleted);
            logger.info("✓ Deleted {} users from Cognito", cognitoUsersDeleted);
        } catch (Exception e) {
            logger.error("Failed to delete Cognito users (continuing anyway): {}", e.getMessage());
            counts.put("cognito_users", 0);
        }

        // 3. Delete ALL data from PostgreSQL (in dependency order)
        logger.warn("Step 3/3: Deleting all data from PostgreSQL...");

        // Delete likes first (no dependencies)
        int likes = (int) likeRepository.count();
        likeRepository.deleteAll();
        counts.put("db_likes", likes);
        logger.info("✓ Deleted {} likes", likes);

        // Delete comments
        int comments = (int) commentRepository.count();
        commentRepository.deleteAll();
        counts.put("db_comments", comments);
        logger.info("✓ Deleted {} comments", comments);

        // Delete files (attached to posts)
        int files = (int) fileRepository.count();
        fileRepository.deleteAll();
        counts.put("db_files", files);
        logger.info("✓ Deleted {} file records", files);

        // Delete posts
        int posts = (int) postRepository.count();
        postRepository.deleteAll();
        counts.put("db_posts", posts);
        logger.info("✓ Deleted {} posts", posts);

        // Delete subjects
        int subjects = (int) subjectRepository.count();
        subjectRepository.deleteAll();
        counts.put("db_subjects", subjects);
        logger.info("✓ Deleted {} subjects", subjects);

        // Delete users (NOW we delete them!)
        int users = (int) userRepository.count();
        userRepository.deleteAll();
        counts.put("db_users", users);
        logger.info("✓ Deleted {} users from database", users);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("deleted", counts);
        result.put("message", "⚠️ ALL DATA WIPED - S3, Cognito, and Database completely cleaned");
        result.put("warning", "This was a destructive operation. All data has been permanently deleted.");

        logger.warn("🔥 COMPLETE WIPEOUT FINISHED: {}", counts);
        return result;
    }

    /**
     * Ensure user exists in both Cognito and database
     * Creates user in Cognito first, then database
     * Idempotent - safe to call multiple times
     */
    private User ensureUserExists(String email, String name) {
        // 1. Check if user already exists in database
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            logger.info("User already exists in database: {}", email);
            return existing.get();
        }

        // 2. Create user in Cognito first (with password: Password123!)
        logger.info("Registering user in AWS Cognito: {}", email);
        String cognitoUserId;
        try {
            cognitoUserId = cognitoService.adminCreateUser(email, TEST_USER_PASSWORD, name);
            logger.info("User registered in Cognito: {} (ID: {})", email, cognitoUserId);
        } catch (Exception e) {
            logger.error("Failed to create user in Cognito: {}", email, e);
            throw new RuntimeException("Failed to register user in Cognito: " + e.getMessage(), e);
        }

        // 3. Create user in database with Cognito link
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setNickName(email.split("@")[0]);  // Use email prefix as nickname (e.g., "kurekadam314")
        user.setCognitoUserId(cognitoUserId);  // Link to Cognito user
        user.setEmailVerified(true);  // Pre-verified
        user.setTwoFactorEnabled(false);
        user = userRepository.save(user);
        logger.info("User created in database: {} (linked to Cognito ID: {})", email, cognitoUserId);

        return user;
    }

    private List<Subject> createSubjects() {
        List<String[]> subjectData = Arrays.asList(
            // 1. ročník
            new String[]{"Základy programovania (1. ročník)", "Úvod do programovania v jazyku C/C++"},
            new String[]{"Matematika I (1. ročník)", "Lineárna algebra, analytická geometria"},
            new String[]{"Architektúra počítačov (1. ročník)", "Hardvér, procesory, pamäte"},
            new String[]{"Diskrétna matematika (1. ročník)", "Logika, množiny, relácie, grafy"},
            // 2. ročník
            new String[]{"Objektovo orientované programovanie (2. ročník)", "OOP v Jave, dedičnosť, polymorfizmus"},
            new String[]{"Databázové systémy (2. ročník)", "SQL, relačné databázy, normalizácia"},
            new String[]{"Operačné systémy (2. ročník)", "Procesy, vlákna, správa pamäte"},
            new String[]{"Počítačové siete (2. ročník)", "TCP/IP, sieťové protokoly, routing"},
            // 3. ročník
            new String[]{"Softvérové inžinierstvo (3. ročník)", "Metodiky vývoja, UML, testovanie"},
            new String[]{"Webové technológie (3. ročník)", "HTML, CSS, JavaScript, frameworky"},
            new String[]{"Umelá inteligencia (3. ročník)", "Strojové učenie, neurónové siete"},
            new String[]{"Bakalársky projekt (3. ročník)", "Záverečná práca a jej obhajoba"}
        );

        List<Subject> subjects = new ArrayList<>();
        for (String[] data : subjectData) {
            Optional<Subject> existing = subjectRepository.findByName(data[0]);
            if (existing.isEmpty()) {
                Subject subject = new Subject();
                subject.setName(data[0]);
                subject.setDescription(data[1]);
                subjects.add(subjectRepository.save(subject));
                logger.info("Created subject: {}", data[0]);
            } else {
                subjects.add(existing.get());
                logger.info("Subject already exists: {}", data[0]);
            }
        }
        return subjects;
    }

    private List<Post> createPosts(User user, List<Subject> subjects) {
        List<Object[]> postData = Arrays.asList(
            new Object[]{0, "Binary Search Tree Implementation Guide", "Complete guide to implementing BST with insertion, deletion, and traversal algorithms"},
            new Object[]{0, "Big O Notation Cheat Sheet", "Reference guide for time and space complexity analysis"},
            new Object[]{1, "Process vs Thread Explained", "Understanding the difference between processes and threads in operating systems"},
            new Object[]{1, "Deadlock Prevention Strategies", "Four conditions for deadlock and how to prevent them"},
            new Object[]{2, "SQL JOIN Types Visual Guide", "Visual explanation of INNER, LEFT, RIGHT, and FULL OUTER joins"},
            new Object[]{2, "Database Normalization Tutorial", "Step-by-step guide to 1NF, 2NF, 3NF, and BCNF"},
            new Object[]{3, "TCP vs UDP - When to Use Each", "Comparing connection-oriented and connectionless protocols"},
            new Object[]{3, "HTTP/2 vs HTTP/3 Performance", "Latest improvements in web protocol performance"},
            new Object[]{4, "SOLID Principles Explained", "Five principles of object-oriented design with examples"},
            new Object[]{4, "Design Patterns Quick Reference", "Common design patterns: Singleton, Factory, Observer, etc."},
            new Object[]{5, "React Hooks Complete Guide", "Comprehensive tutorial on useState, useEffect, and custom hooks"},
            new Object[]{5, "RESTful API Best Practices", "Guidelines for designing clean and scalable REST APIs"},
            new Object[]{6, "Neural Networks from Scratch", "Building a simple neural network in Python"},
            new Object[]{6, "Gradient Descent Visualization", "Interactive visualization of optimization algorithms"},
            new Object[]{7, "CPU Pipeline Explained", "Understanding instruction pipelining and hazards"},
            new Object[]{7, "Cache Memory Hierarchy", "L1, L2, L3 caches and memory access patterns"},
            new Object[]{8, "Graph Theory Basics", "Introduction to graphs, trees, and graph algorithms"},
            new Object[]{8, "Boolean Algebra Fundamentals", "Logic gates and Boolean expressions"},
            new Object[]{9, "Turing Machines Explained", "Introduction to computational theory and Turing completeness"},
            new Object[]{9, "P vs NP Problem Overview", "Understanding computational complexity classes"}
        );

        List<Post> posts = new ArrayList<>();
        for (Object[] data : postData) {
            int subjectIndex = (int) data[0];
            String title = (String) data[1];
            String description = (String) data[2];

            Post post = new Post();
            post.setTitle(title);
            post.setDescription(description);
            post.setUser(user);
            post.setSubject(subjects.get(subjectIndex));
            post.setNumberOfLikes(0);
            post.setNumberOfComments(0);
            post.setIsPublished(true);
            posts.add(postRepository.save(post));
        }

        logger.info("Created {} posts", posts.size());
        return posts;
    }

    private List<Comment> createComments(User mainUser, User fallbackUser, List<Post> posts) {
        String[] commentTexts = {
            "Great explanation! This really helped me understand the concept.",
            "Could you add more examples for edge cases?",
            "This is exactly what I needed for my assignment, thank you!",
            "Very clear and concise. Bookmarking this for later.",
            "I implemented this and it works perfectly. Thanks for sharing!",
            "Minor typo in line 3, but otherwise excellent content.",
            "Can you explain the time complexity analysis in more detail?",
            "This approach is more efficient than what we learned in class.",
            "Do you have any recommended resources to learn more?",
            "I was struggling with this topic, but your post made it click!",
            "Quick question: does this work with large datasets?",
            "Shared this with my study group, everyone found it helpful.",
            "The visualizations make this so much easier to understand.",
            "Would love to see a follow-up post on advanced techniques.",
            "This is production-ready code, great job!",
            "I noticed a potential optimization in the algorithm...",
            "Can this be adapted for distributed systems?",
            "Perfect timing! We have an exam on this next week.",
            "Your explanations are always so clear and well-structured.",
            "I compared this with other solutions and yours is the best."
        };

        List<Comment> comments = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility

        // Create 2 comments per post on average
        for (Post post : posts) {
            int numComments = 1 + random.nextInt(3); // 1-3 comments per post
            for (int i = 0; i < numComments; i++) {
                Comment comment = new Comment();
                comment.setPost(post);
                // Alternate between users
                comment.setUser(i % 2 == 0 ? mainUser : fallbackUser);
                comment.setContent(commentTexts[random.nextInt(commentTexts.length)]);
                comment.setNumberOfLikes(0);
                comments.add(commentRepository.save(comment));
            }

            // Update post comment count
            post.setNumberOfComments(numComments);
            postRepository.save(post);
        }

        logger.info("Created {} comments", comments.size());
        return comments;
    }

    private int createLikes(User mainUser, User fallbackUser, List<Post> posts, List<Comment> comments) {
        Random random = new Random(42);
        int likesCount = 0;

        // Like ~60% of posts
        for (Post post : posts) {
            if (random.nextDouble() < 0.6) {
                // Mainuser likes some posts
                if (random.nextBoolean()) {
                    createLike(mainUser, post, null);
                    post.setNumberOfLikes(post.getNumberOfLikes() + 1);
                    likesCount++;
                }
                // Fallback user likes some posts
                if (random.nextBoolean()) {
                    createLike(fallbackUser, post, null);
                    post.setNumberOfLikes(post.getNumberOfLikes() + 1);
                    likesCount++;
                }
                postRepository.save(post);
            }
        }

        // Like ~40% of comments
        for (Comment comment : comments) {
            if (random.nextDouble() < 0.4) {
                // Don't like your own comment
                User liker = comment.getUser().equals(mainUser) ? fallbackUser : mainUser;
                createLike(liker, null, comment);
                comment.setNumberOfLikes(comment.getNumberOfLikes() + 1);
                commentRepository.save(comment);
                likesCount++;
            }
        }

        logger.info("Created {} likes", likesCount);
        return likesCount;
    }

    private void createLike(User user, Post post, Comment comment) {
        // Check if like already exists
        if (post != null) {
            if (likeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
                return;
            }
        }
        if (comment != null) {
            if (likeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())) {
                return;
            }
        }

        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setComment(comment);
        likeRepository.save(like);
    }
}
