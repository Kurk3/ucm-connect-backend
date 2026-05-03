package com.example.ucmconnectapi.controller;

import com.example.ucmconnectapi.dto.SubjectDTO;
import com.example.ucmconnectapi.dto.SubjectListResponse;
import com.example.ucmconnectapi.exception.ErrorResponse;
import com.example.ucmconnectapi.service.SubjectService;
import com.example.ucmconnectapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Subject endpoints
 * Manages study subjects/categories
 */
@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subjects", description = "Endpoints for managing study subjects/categories")
public class SubjectController {

    private final SubjectService subjectService;
    private final UserService userService;

    @Autowired
    public SubjectController(SubjectService subjectService, UserService userService) {
        this.subjectService = subjectService;
        this.userService = userService;
    }

    /**
     * GET /subjects - Get all subjects
     */
    @GetMapping
    @Operation(
            summary = "Get all subjects",
            description = "Retrieve a list of all available study subjects/categories"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved subjects",
                    content = @Content(schema = @Schema(implementation = SubjectListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SubjectListResponse> getAllSubjects() {
        SubjectListResponse response = subjectService.getAllSubjects();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /subjects/{id} - Get subject by ID
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get subject by ID",
            description = "Retrieve a specific subject by its UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved subject",
                    content = @Content(schema = @Schema(implementation = SubjectDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SubjectDTO> getSubjectById(@PathVariable UUID id) {
        SubjectDTO subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subject);
    }

    /**
     * POST /subjects - Create new subject (Admin only)
     */
    @PostMapping
    @Operation(
            summary = "Create new subject (Admin only)",
            description = "Create a new study subject/category. Only administrators can create subjects."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Subject created successfully",
                    content = @Content(schema = @Schema(implementation = SubjectDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only admins can create subjects",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Subject with this name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SubjectDTO> createSubject(@Valid @RequestBody SubjectDTO subjectDTO) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        SubjectDTO createdSubject = subjectService.createSubject(subjectDTO, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubject);
    }

    /**
     * PUT /subjects/{id} - Update subject (Admin only)
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update subject (Admin only)",
            description = "Update an existing subject by its UUID. Only administrators can update subjects."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Subject updated successfully",
                    content = @Content(schema = @Schema(implementation = SubjectDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only admins can update subjects",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Subject with this name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SubjectDTO> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectDTO subjectDTO
    ) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        SubjectDTO updatedSubject = subjectService.updateSubject(id, subjectDTO, currentUserId);
        return ResponseEntity.ok(updatedSubject);
    }

    /**
     * DELETE /subjects/{id} - Delete subject (Admin only)
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete subject (Admin only)",
            description = "Delete a subject by its UUID. Only administrators can delete subjects."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Subject deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Only admins can delete subjects",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subject not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id) {
        UUID currentUserId = userService.getCurrentUserIdFromToken();
        subjectService.deleteSubject(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
