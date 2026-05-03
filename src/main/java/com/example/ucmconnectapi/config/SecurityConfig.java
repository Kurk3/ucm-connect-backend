package com.example.ucmconnectapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${aws.cognito.jwks-url}")
    private String jwksUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for REST API with JWT)
            .csrf(csrf -> csrf.disable())

            // Security headers (HSTS, X-Content-Type-Options, X-Frame-Options)
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentTypeOptions(contentType -> {})
                .frameOptions(frame -> frame.deny())
            )

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure authorization rules - DENY BY DEFAULT (Best Practice 2025)
            .authorizeHttpRequests(auth -> auth
                // ✅ PUBLIC: Auth endpoints (registration, login, password reset, refresh token)
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/verify-email", "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()

                // 🔒 PROTECTED: Logout requires authentication
                .requestMatchers("/api/v1/auth/logout").authenticated()

                // ✅ PUBLIC: Actuator health endpoints (for AWS ELB health checks)
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()

                // ✅ PUBLIC: Swagger/OpenAPI documentation
                .requestMatchers("/api/v1/swagger-ui/**", "/api/v1/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/api-docs/**", "/v3/api-docs/**").permitAll()

                // ✅ PUBLIC: Discord OAuth2 callback (redirect from Discord)
                .requestMatchers("/api/v1/discord/callback", "/api/v1/discord/auth-url").permitAll()


                // 🔒 PROTECTED: Test endpoints (admin check done in controller via DB role)
                .requestMatchers("/api/v1/test/seed", "/api/v1/test/public", "/api/v1/test/seed-subjects", "/api/v1/test/drop").authenticated()

                // ✅ PUBLIC: H2 Console and error pages (development only)
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 🔒 PROTECTED: Everything else requires JWT authentication
                // This includes: /users/**, /subjects/**, /posts/**, /comments/**, /files/**
                // AND /api/v1/test/protected (protected test endpoint)
                .anyRequest().authenticated()
            )

            // Use JWT for authentication (OAuth2 Resource Server)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )

            // Stateless session management (no HTTP sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Allow H2 Console frames (only for development)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }

    /**
     * JWT Decoder that validates tokens using AWS Cognito JWKS endpoint
     * Only enabled when AWS Cognito is configured
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "aws.cognito.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
    }

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    /**
     * CORS Configuration - allows frontend to call API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
