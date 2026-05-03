package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.entity.*;
import com.example.ucmconnectapi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Seeds production database with real study materials from UCM students.
 * Files are loaded from the seed-data/ directory in the project root.
 */
@Service
public class ProductionDataSeedService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionDataSeedService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private LikeRepository likeRepository;
    @Autowired private FileRepository fileRepository;
    @Autowired private FileService fileService;
    @Autowired private S3FileStorageService s3FileStorageService;

    private static final String SEED_DIR = "seed-data";

    @Transactional
    public Map<String, Object> seedProductionData() {
        logger.info("Starting production data seeding...");

        Map<String, Integer> counts = new HashMap<>();

        // 1. Ensure both users exist
        User adam = ensureUser("2825012@ucm.sk", "Adam Kurek", "adam.kurek",
                "434488b2-7011-70d9-ec1c-999d896cb563");
        User folty = ensureUser("2827298@ucm.sk", "Andrej Folta", "folty",
                "a38498a2-50f1-70cf-d6e3-edd0adebf37d");
        counts.put("users", 2);

        // 2. Find subjects
        Map<String, Subject> subjects = new HashMap<>();
        for (Subject s : subjectRepository.findAll()) {
            subjects.put(s.getName().toLowerCase(), s);
        }

        // 3. Create posts with files
        int postsCreated = 0;
        int filesUploaded = 0;
        int commentsCreated = 0;
        int likesCreated = 0;

        // --- Adam's posts ---
        postsCreated += createPostWithFile(adam, subjects, "algoritmy a dátové štruktúry i",
                "Algoritmy a datove struktury I - poznamky",
                "Poznamky z prednasky, zakladne pojmy, triedenie a vyhladavanie",
                "algoritmy_1.pdf");

        postsCreated += createPostWithFile(adam, subjects, "algoritmy a dátové štruktúry i",
                "Algoritmus triedenia - prehladovy dokument",
                "Prehladovy dokument o roznych algoritmoch triedenia s porovnanim zlozitosti",
                "algoritmus_triedenia.pdf");

        postsCreated += createPostWithFile(adam, subjects, "teoretické základy informatiky ii",
                "Algoritmy - Teoreticke zaklady informatiky II",
                "Poznamky z TZI 2 zamerane na zlozitost algoritmov, grafy a automaty",
                "algoritmy_tzi_2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "teoretické základy informatiky ii",
                "TZI 2 - export poznamok",
                "Kompletny export poznamok z predmetu Teoreticke zaklady informatiky II",
                "tzi2_export.pdf");

        postsCreated += createPostWithFile(adam, subjects, "teoretické základy informatiky ii",
                "Grafy zlozitosti - TZI 2",
                "Grafy casovej a priestorovej zlozitosti algoritmov",
                "grafy_zlozitosti_tzi2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Databazy 1 - zapocty a ucenie",
                "Materialy na pripravu k zapoctom z databazovych systemov",
                "db_1_zapocty_ucenie.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Databazy 2 - prakticka cast",
                "Prakticky material k predmetu Databazy 2, SQL dopyty a procedury",
                "db_2_prakticka_cast.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Optimalizacie v databazach",
                "Materialy o optimalizacii SQL dopytov, indexy a execution plans",
                "optimalizacie_db_2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Oracle Scheduler - Databazy 2",
                "Poznamky o Oracle Scheduler, automatizacia uloh v databaze",
                "oracle_scheduler_db_2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Pisomka z Databaz 2 - vzorove otazky",
                "Otazky podobne ako boli na pisomke z Databaz 2",
                "pisomka_db_2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Podobny test - Databazy 2",
                "Podobny test ako bol na skuske z Databaz 2, CBO, indexy",
                "podobny_test_db_2.pdf");

        postsCreated += createPostWithFile(adam, subjects, "počítačová grafika i",
                "Pocitacova grafika - poznamky",
                "Zdielane Notion poznamky z pocitacovej grafiky",
                "grafika.pdf");

        postsCreated += createPostWithFile(adam, subjects, "databázové systémy",
                "Databazy 2 - vzorove otazky z pisomky",
                "Otazky CBO, indexy, procedury, funkcie",
                "databazy_2.pdf");

        // --- Folty's posts ---
        postsCreated += createPostWithFile(folty, subjects, "digitálna forenzná analýza",
                "Forenzna analyza - poznamky",
                "Poznamky z prednasok digitalnej forenznej analyzy",
                "forenzna.pdf");

        postsCreated += createPostWithFile(folty, subjects, "počítačové siete ii",
                "Siete 2 - teoria",
                "Teoreticke poznamky z pocitacovych sieti II, protokoly a routing",
                "siete2_teoria.pdf");

        postsCreated += createPostWithFile(folty, subjects, "softvérové inžinierstvo",
                "Softverove inzinierstvo - poznamky",
                "Poznamky z prednasky softveroveho inzinierstva, metodiky vyvoja",
                "softverove_poznamky.pdf");

        postsCreated += createPostWithFile(folty, subjects, "systémy virtuálnej a zmiešanej reality",
                "Systemy virtualnej reality - poznamky",
                "Material o VR/AR systemoch a ich vyuziti",
                "systemy_virtualnej_reality.pdf");

        postsCreated += createPostWithFile(folty, subjects, "základy práva pre informatikov",
                "Pravo pre informatikov - poznamky",
                "Poznamky zo zakonu o autorskych pravach, GDPR a kybernetickej bezpecnosti",
                "pravo.pdf");

        postsCreated += createPostWithFile(folty, subjects, "programovanie i",
                "Zadanie c. 18 - Adam Kurek",
                "Vzorove riesenie zadania c. 18 z programovania",
                "Zadanie_c_18_-_Adam_Kurek.pdf");

        counts.put("posts", postsCreated);
        counts.put("files", postsCreated); // 1 file per post

        // 4. Add comments between users
        List<Post> allPosts = postRepository.findAll();
        Random rng = new Random(42);
        String[] commentTexts = {
                "Super material, dakujem za zdielanie!",
                "Toto mi pomohlo pri priprave na zapocet.",
                "Vies pridat aj riesenia prikladov?",
                "Presne toto som hladal, dik!",
                "Pouzil som to na skusku a dostal som A.",
                "Mohol by si doplnit este teoreticku cast?",
                "Velmi prehladne spracovane, odporucam vsetkym.",
                "Mali sme to na prednaske minuly tyzden, dobre poznamky.",
                "Doplnil by som este cast o indexoch.",
                "Funguje to aj pre PostgreSQL alebo len Oracle?"
        };

        for (Post post : allPosts) {
            int numComments = 1 + rng.nextInt(3);
            for (int i = 0; i < numComments; i++) {
                User commenter = post.getUser().equals(adam) ? folty : adam;
                if (rng.nextBoolean()) commenter = post.getUser().equals(adam) ? folty : adam;

                Comment comment = new Comment();
                comment.setPost(post);
                comment.setUser(commenter);
                comment.setContent(commentTexts[rng.nextInt(commentTexts.length)]);
                comment.setNumberOfLikes(0);
                commentRepository.save(comment);
                commentsCreated++;
            }
            post.setNumberOfComments(numComments);
            postRepository.save(post);
        }
        counts.put("comments", commentsCreated);

        // 5. Add likes
        for (Post post : allPosts) {
            if (rng.nextDouble() < 0.7) {
                createLikeIfNotExists(adam, post, null);
                post.setNumberOfLikes(post.getNumberOfLikes() + 1);
                likesCreated++;
            }
            if (rng.nextDouble() < 0.7) {
                createLikeIfNotExists(folty, post, null);
                post.setNumberOfLikes(post.getNumberOfLikes() + 1);
                likesCreated++;
            }
            postRepository.save(post);
        }
        counts.put("likes", likesCreated);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("created", counts);
        logger.info("Production data seeding completed: {}", counts);
        return result;
    }

    /**
     * Drop all production data — likes, comments, files (S3 + DB), posts, users.
     * Subjects are preserved.
     */
    @Transactional
    public Map<String, Object> dropProductionData() {
        logger.warn("Dropping all production data...");
        Map<String, Integer> counts = new HashMap<>();

        int likes = (int) likeRepository.count();
        likeRepository.deleteAll();
        counts.put("likes", likes);

        int comments = (int) commentRepository.count();
        commentRepository.deleteAll();
        counts.put("comments", comments);

        // Delete files from S3 first, then DB
        int files = (int) fileRepository.count();
        try {
            s3FileStorageService.deleteAllFilesInBucket();
        } catch (Exception e) {
            logger.error("Failed to delete S3 files: {}", e.getMessage());
        }
        fileRepository.deleteAll();
        counts.put("files", files);

        int posts = (int) postRepository.count();
        postRepository.deleteAll();
        counts.put("posts", posts);

        int users = (int) userRepository.count();
        userRepository.deleteAll();
        counts.put("users", users);

        logger.warn("Production data dropped: {}", counts);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("deleted", counts);
        return result;
    }

    private User ensureUser(String email, String name, String nickName, String cognitoId) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!cognitoId.equals(user.getCognitoUserId())) {
                user.setCognitoUserId(cognitoId);
                user = userRepository.save(user);
            }
            logger.info("User exists: {} ({})", email, user.getId());
            return user;
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setNickName(nickName);
        user.setCognitoUserId(cognitoId);
        user.setEmailVerified(true);
        user.setTwoFactorEnabled(false);
        user = userRepository.save(user);
        logger.info("Created user: {} ({})", email, user.getId());
        return user;
    }

    private int createPostWithFile(User user, Map<String, Subject> subjects,
                                   String subjectKey, String title, String description,
                                   String fileName) {
        // Check if post already exists
        if (postRepository.findAll().stream()
                .anyMatch(p -> p.getTitle().equals(title) && p.getUser().equals(user))) {
            logger.info("Post already exists: {}", title);
            return 0;
        }

        Subject subject = subjects.get(subjectKey);
        if (subject == null) {
            logger.warn("Subject not found: '{}', skipping post: {}", subjectKey, title);
            return 0;
        }

        // Create post
        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setUser(user);
        post.setSubject(subject);
        post.setNumberOfLikes(0);
        post.setNumberOfComments(0);
        post.setIsPublished(true);
        post = postRepository.save(post);

        // Upload file if it exists
        Path filePath = Paths.get(SEED_DIR, fileName);
        if (Files.exists(filePath)) {
            try {
                byte[] content = Files.readAllBytes(filePath);
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) mimeType = "application/pdf";

                MultipartFile multipartFile = new InMemoryMultipartFile(fileName, mimeType, content);
                fileService.uploadFile(post.getId(), multipartFile, user.getId());
                logger.info("Uploaded file: {} for post: {}", fileName, title);
            } catch (IOException e) {
                logger.error("Failed to upload file {}: {}", fileName, e.getMessage());
            }
        } else {
            logger.warn("File not found: {}, creating post without file", filePath);
        }

        logger.info("Created post: {} (subject: {})", title, subject.getName());
        return 1;
    }

    /**
     * Simple MultipartFile implementation for in-memory file data.
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;

        InMemoryMultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    private void createLikeIfNotExists(User user, Post post, Comment comment) {
        if (post != null && likeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            return;
        }
        if (comment != null && likeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())) {
            return;
        }
        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        like.setComment(comment);
        likeRepository.save(like);
    }
}
