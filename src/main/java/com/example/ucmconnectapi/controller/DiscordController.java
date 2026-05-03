package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.repository.UserRepository;
import com.example.ucmconnectapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/discord")
public class DiscordController {

    private static final Logger log = LoggerFactory.getLogger(DiscordController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${discord.client-id:}")
    private String clientId;

    @Value("${discord.client-secret:}")
    private String clientSecret;

    @Value("${discord.redirect-uri:}")
    private String redirectUri;

    @Value("${discord.bot-api-url:http://localhost:3500}")
    private String botApiUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public DiscordController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Step 1: Frontend calls this to get the Discord OAuth2 URL
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = "https://discord.com/oauth2/authorize"
            + "?client_id=" + clientId
            + "&response_type=code"
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
            + "&scope=identify+guilds.join";

        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Step 2: Discord redirects here after user authorizes.
     * Exchanges code for token, gets Discord user info, saves to DB, calls bot.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code) {
        try {
            // Exchange code for access token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "https://discord.com/api/v10/oauth2/token", request, Map.class);

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            // Get Discord user info
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://discord.com/api/v10/users/@me", HttpMethod.GET, userRequest, Map.class);

            String discordId = (String) userResponse.getBody().get("id");
            String discordUsername = (String) userResponse.getBody().get("username");

            log.info("Discord OAuth2 callback - Discord user: {} ({})", discordUsername, discordId);

            // Call bot to add user to guild using OAuth2 access token
            try {
                HttpHeaders botHeaders = new HttpHeaders();
                botHeaders.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> botBody = Map.of(
                    "discord_id", discordId,
                    "access_token", accessToken,
                    "nickname", discordUsername
                );

                restTemplate.postForEntity(
                    botApiUrl + "/add-member",
                    new HttpEntity<>(botBody, botHeaders),
                    Map.class
                );

                log.info("Bot notified to add {} to guild", discordId);
            } catch (Exception e) {
                log.warn("Could not notify bot to add member: {}", e.getMessage());
            }

            String redirect = frontendUrl + "/discord/linked?discord_id=" + discordId
                + "&discord_username=" + URLEncoder.encode(discordUsername, StandardCharsets.UTF_8);

            return ResponseEntity.status(302)
                .header("Location", redirect)
                .build();

        } catch (Exception e) {
            log.error("Discord OAuth2 error: {}", e.getMessage(), e);
            return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/discord/error")
                .build();
        }
    }

    /**
     * Step 3: Frontend calls this (with JWT auth) to save discord_id to user
     */
    @PostMapping("/link")
    public ResponseEntity<Map<String, Object>> linkDiscord(@RequestBody Map<String, String> body) {
        String discordId = body.get("discord_id");
        if (discordId == null || discordId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "discord_id is required"));
        }

        User user = userService.getCurrentAuthenticatedUser();
        user.setDiscordId(discordId);
        userRepository.save(user);

        log.info("User {} linked Discord ID: {}", user.getNickName(), discordId);

        // Call bot to add role
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> botBody = Map.of(
                "discord_id", discordId,
                "nickname", user.getNickName()
            );

            restTemplate.postForEntity(
                botApiUrl + "/add-member",
                new HttpEntity<>(botBody, headers),
                Map.class
            );

            log.info("Bot notified for user {} ({})", user.getNickName(), discordId);
        } catch (Exception e) {
            log.warn("Could not notify bot: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "status", "linked",
            "discordId", discordId
        ));
    }

    /**
     * Check if current user has Discord linked
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        User user = userService.getCurrentAuthenticatedUser();
        boolean linked = user.getDiscordId() != null && !user.getDiscordId().isEmpty();

        return ResponseEntity.ok(Map.of(
            "linked", linked,
            "discordId", linked ? user.getDiscordId() : ""
        ));
    }
}
