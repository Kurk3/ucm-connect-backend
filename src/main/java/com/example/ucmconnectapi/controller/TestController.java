package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.ErrorResponse;
import com.example.ucmconnectapi.entity.Subject;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.entity.Role;
import com.example.ucmconnectapi.repository.SubjectRepository;
import com.example.ucmconnectapi.repository.UserRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test Endpoints", description = "Test endpoints for JWT authentication validation")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Operation(
            summary = "Public test endpoint",
            description = "Test endpoint accessible without authentication. Anyone can access this."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        logger.debug("Public endpoint accessed");

        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint. No authentication required!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Protected test endpoint",
            description = "Test endpoint that requires JWT authentication. Use this to verify your JWT token works correctly."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint() {
        logger.debug("Protected endpoint accessed");

        // Get currently authenticated user
        User currentUser = userService.getCurrentAuthenticatedUser();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "You are authenticated! Your JWT token is valid.");
        response.put("userId", currentUser.getId().toString());
        response.put("email", currentUser.getEmail());
        response.put("name", currentUser.getName());
        response.put("cognitoSub", currentUser.getCognitoUserId());
        response.put("timestamp", System.currentTimeMillis());

        logger.debug("Protected endpoint accessed by user: {}", currentUser.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Seed KAI subjects",
            description = "Seed initial subjects for KAI (1st, 2nd, 3rd year + optional subjects). Public endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subjects seeded successfully",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/seed-subjects")
    public ResponseEntity<Map<String, Object>> seedSubjects() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only admins can seed subjects"));
        }
        logger.info("Seeding KAI subjects and admin users...");

        // Seed admin users with hardcoded Cognito IDs
        createAdminUserIfNotExists("2825012@ucm.sk", "Adam Kurek", "adam.kurek", "434488b2-7011-70d9-ec1c-999d896cb563");
        createAdminUserIfNotExists("2827298@ucm.sk", "Andrej Folta", "folty", "a38498a2-50f1-70cf-d6e3-edd0adebf37d");

        List<String> createdSubjects = new ArrayList<>();
        int skippedCount = 0;

        // 1. ROČNÍK - ZIMNÝ SEMESTER
        skippedCount += createSubjectIfNotExists("počítačové siete I", "1. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("programovanie I", "1. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("matematické základy informatiky", "1. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("internetové technológie", "1. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("úvod do štúdia informatiky", "1. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("anglický jazyk pre informatikov I", "1. ročník, zimný semester", createdSubjects);

        // 1. ROČNÍK - LETNÝ SEMESTER
        skippedCount += createSubjectIfNotExists("algoritmy a dátové štruktúry I", "1. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("programovanie II", "1. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("algebra a diskrétna matematika pre informatikov", "1. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("pokročilé internetové technológie", "1. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("anglický jazyk pre informatikov II", "1. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("systémy virtuálnej a zmiešanej reality", "1. ročník, letný semester", createdSubjects);

        // 2. ROČNÍK - ZIMNÝ SEMESTER
        skippedCount += createSubjectIfNotExists("operačné systémy", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("algoritmy a dátové štruktúry II", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("úvod do databázových systémov", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("teoretické základy informatiky I", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("počítačová grafika I", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("moderné programovacie jazyky", "2. ročník, zimný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("tímový projekt", "2. ročník, zimný semester", createdSubjects);

        // 2. ROČNÍK - LETNÝ SEMESTER
        skippedCount += createSubjectIfNotExists("počítačové architektúry", "2. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("počítačové siete II", "2. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("databázové systémy", "2. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("teoretické základy informatiky II", "2. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("elektrotechnika a elektronika", "2. ročník, letný semester", createdSubjects);
        skippedCount += createSubjectIfNotExists("ročníková práca", "2. ročník, letný semester", createdSubjects);

        // 3. ROČNÍK
        skippedCount += createSubjectIfNotExists("mobilné technológie", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("softvérové inžinierstvo", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("bakalársky projekt I", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("úvod do tvorivej umelej inteligencie", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("informačná bezpečnosť", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("modelovanie a simulácia v prostredí Matlab", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("projektový manažment", "3. ročník", createdSubjects);
        skippedCount += createSubjectIfNotExists("bakalársky projekt II", "3. ročník", createdSubjects);

        // VOLITEĽNÉ PREDMETY - Blok aplikovaná informatika
        skippedCount += createSubjectIfNotExists("inteligentné techniky v e-learningu", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("základy podnikania a manažmentu", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("bioinformatika", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("systémy DTP", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("pokročilé programovanie", "Voliteľný predmet (2. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("internet vecí", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("počítačová grafika II", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("HPC a cloudové počítanie", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("počítačové siete III", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("funkcionálne programovanie", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("geografické informačné systémy", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("aplikačné informačné systémy", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("odborná prax v IT I", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("tvorba počítačových hier", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("odborná prax v IT II", "Voliteľný predmet (3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("základy práva pre informatikov", "Voliteľný predmet (2.-3. ročník)", createdSubjects);
        skippedCount += createSubjectIfNotExists("digitálna forenzná analýza", "Voliteľný predmet (2.-3. ročník)", createdSubjects);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subject seeding completed");
        response.put("totalCreated", createdSubjects.size());
        response.put("totalSkipped", skippedCount);
        response.put("createdSubjects", createdSubjects);

        logger.info("Seeding completed: {} created, {} skipped", createdSubjects.size(), skippedCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to create subject if it doesn't exist
     * Returns 1 if skipped, 0 if created
     */
    private int createSubjectIfNotExists(String name, String description, List<String> createdSubjects) {
        if (subjectRepository.existsByName(name)) {
            logger.debug("Subject already exists, skipping: {}", name);
            return 1;
        }

        Subject subject = new Subject();
        subject.setName(name);
        subject.setDescription(description);
        subjectRepository.save(subject);
        createdSubjects.add(name);
        logger.debug("Created subject: {}", name);
        return 0;
    }

    private void createAdminUserIfNotExists(String email, String name, String nickName, String cognitoUserId) {
        if (userRepository.findByEmail(email).isPresent()) {
            // Update to admin if exists but not admin
            User existing = userRepository.findByEmail(email).get();
            if (existing.getRole() != Role.ADMIN) {
                existing.setRole(Role.ADMIN);
                userRepository.save(existing);
                logger.info("Updated user to ADMIN: {}", email);
            }
            if (existing.getCognitoUserId() == null || !existing.getCognitoUserId().equals(cognitoUserId)) {
                existing.setCognitoUserId(cognitoUserId);
                userRepository.save(existing);
                logger.info("Updated Cognito ID for user: {}", email);
            }
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setNickName(nickName);
        user.setCognitoUserId(cognitoUserId);
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        logger.info("Created admin user: {}", email);
    }
}
