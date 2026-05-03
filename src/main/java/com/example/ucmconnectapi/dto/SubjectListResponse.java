package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response wrapper for list of subjects
 */
@Schema(description = "Subject list response")
public class SubjectListResponse {

    @Schema(description = "List of subjects")
    private List<SubjectDTO> subjects;

    @Schema(description = "Total number of subjects", example = "15")
    private Integer total;

    // Constructors
    public SubjectListResponse() {
    }

    public SubjectListResponse(List<SubjectDTO> subjects, Integer total) {
        this.subjects = subjects;
        this.total = total;
    }

    // Getters and Setters
    public List<SubjectDTO> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectDTO> subjects) {
        this.subjects = subjects;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
