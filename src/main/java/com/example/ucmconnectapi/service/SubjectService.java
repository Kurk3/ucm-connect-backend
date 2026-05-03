package com.example.ucmconnectapi.service;

import com.example.ucmconnectapi.dto.SubjectDTO;
import com.example.ucmconnectapi.dto.SubjectListResponse;
import com.example.ucmconnectapi.entity.Subject;
import com.example.ucmconnectapi.entity.User;
import com.example.ucmconnectapi.exception.ConflictException;
import com.example.ucmconnectapi.exception.ForbiddenException;
import com.example.ucmconnectapi.exception.ResourceNotFoundException;
import com.example.ucmconnectapi.repository.SubjectRepository;
import com.example.ucmconnectapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing subjects/categories
 * Contains business logic for subject operations
 */
@Service
public class SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectService.class);

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    @Autowired
    public SubjectService(SubjectRepository subjectRepository, UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all subjects (cached for 24 hours)
     *
     * @return SubjectListResponse containing all subjects and total count
     */
    @Cacheable(value = "subjects")
    @Transactional(readOnly = true)
    public SubjectListResponse getAllSubjects() {
        log.info("⚠️ CACHE MISS - Fetching all subjects from database");
        List<Subject> subjects = subjectRepository.findAll();

        List<SubjectDTO> subjectDTOs = subjects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("✅ Cached {} subjects", subjectDTOs.size());
        return new SubjectListResponse(subjectDTOs, subjectDTOs.size());
    }

    /**
     * Get subject by ID (cached for 24 hours)
     *
     * @param id Subject UUID
     * @return SubjectDTO
     * @throws ResourceNotFoundException if subject not found
     */
    @Cacheable(value = "subjectById", key = "#id")
    @Transactional(readOnly = true)
    public SubjectDTO getSubjectById(UUID id) {
        log.info("⚠️ CACHE MISS - Fetching subject {} from database", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));

        return convertToDTO(subject);
    }

    /**
     * Create new subject (Admin only, invalidates all subject caches)
     *
     * @param subjectDTO Subject data
     * @param currentUserId User performing the action (must be admin)
     * @return Created SubjectDTO
     * @throws ForbiddenException if user is not admin
     * @throws ConflictException if subject with same name already exists
     */
    @CacheEvict(value = {"subjects", "subjectById"}, allEntries = true)
    @Transactional
    public SubjectDTO createSubject(SubjectDTO subjectDTO, UUID currentUserId) {
        log.info("Creating new subject with name: {} by user: {}", subjectDTO.getName(), currentUserId);

        // Verify admin role
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!user.isAdmin()) {
            log.warn("User {} attempted to create subject without admin privileges", currentUserId);
            throw new ForbiddenException("Only admins can create subjects");
        }

        // Check if subject with same name already exists
        if (subjectRepository.existsByName(subjectDTO.getName())) {
            log.warn("Subject with name '{}' already exists", subjectDTO.getName());
            throw new ConflictException("Subject", "name", subjectDTO.getName());
        }

        // Create new subject entity
        Subject subject = new Subject();
        subject.setName(subjectDTO.getName().trim());
        subject.setDescription(subjectDTO.getDescription() != null ?
                subjectDTO.getDescription().trim() : null);

        // Save to database
        Subject savedSubject = subjectRepository.save(subject);
        log.info("Subject created successfully with id: {} by admin: {}", savedSubject.getId(), currentUserId);

        return convertToDTO(savedSubject);
    }

    /**
     * Update existing subject (Admin only, invalidates all subject caches)
     *
     * @param id Subject UUID
     * @param subjectDTO Updated subject data
     * @param currentUserId User performing the action (must be admin)
     * @return Updated SubjectDTO
     * @throws ForbiddenException if user is not admin
     * @throws ResourceNotFoundException if subject not found
     * @throws ConflictException if new name conflicts with existing subject
     */
    @CacheEvict(value = {"subjects", "subjectById"}, allEntries = true)
    @Transactional
    public SubjectDTO updateSubject(UUID id, SubjectDTO subjectDTO, UUID currentUserId) {
        log.info("Updating subject with id: {} by user: {}", id, currentUserId);

        // Verify admin role
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!user.isAdmin()) {
            log.warn("User {} attempted to update subject without admin privileges", currentUserId);
            throw new ForbiddenException("Only admins can update subjects");
        }

        // Check if subject exists
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));

        // Check if new name conflicts with another subject
        if (!subject.getName().equals(subjectDTO.getName())) {
            if (subjectRepository.existsByName(subjectDTO.getName())) {
                log.warn("Cannot update: Subject with name '{}' already exists", subjectDTO.getName());
                throw new ConflictException("Subject", "name", subjectDTO.getName());
            }
        }

        // Update fields
        subject.setName(subjectDTO.getName().trim());
        subject.setDescription(subjectDTO.getDescription() != null ?
                subjectDTO.getDescription().trim() : null);

        // Save changes
        Subject updatedSubject = subjectRepository.save(subject);
        log.info("Subject updated successfully: {} by admin: {}", updatedSubject.getId(), currentUserId);

        return convertToDTO(updatedSubject);
    }

    /**
     * Delete subject (Admin only, invalidates all subject caches)
     *
     * @param id Subject UUID
     * @param currentUserId User performing the action (must be admin)
     * @throws ForbiddenException if user is not admin
     * @throws ResourceNotFoundException if subject not found
     */
    @CacheEvict(value = {"subjects", "subjectById"}, allEntries = true)
    @Transactional
    public void deleteSubject(UUID id, UUID currentUserId) {
        log.info("Deleting subject with id: {} by user: {}", id, currentUserId);

        // Verify admin role
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (!user.isAdmin()) {
            log.warn("User {} attempted to delete subject without admin privileges", currentUserId);
            throw new ForbiddenException("Only admins can delete subjects");
        }

        // Check if subject exists
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject", "id", id);
        }

        subjectRepository.deleteById(id);
        log.info("Subject deleted successfully: {} by admin: {}", id, currentUserId);
    }

    /**
     * Convert Subject entity to SubjectDTO
     */
    private SubjectDTO convertToDTO(Subject subject) {
        SubjectDTO dto = new SubjectDTO();
        dto.setId(subject.getId());
        dto.setName(subject.getName());
        dto.setDescription(subject.getDescription());
        dto.setCreatedAt(subject.getCreatedAt());
        dto.setUpdatedAt(subject.getUpdatedAt());
        return dto;
    }
}
