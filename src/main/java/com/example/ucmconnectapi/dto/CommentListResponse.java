package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response wrapper for paginated list of comments
 */
@Schema(description = "Paginated list of comments")
public class CommentListResponse {

    @Schema(description = "List of comments")
    private List<CommentDTO> comments;

    @Schema(description = "Total number of comments", example = "150")
    private Integer total;

    @Schema(description = "Current page number", example = "1")
    private Integer page;

    @Schema(description = "Items per page", example = "20")
    private Integer limit;

    // Constructors
    public CommentListResponse() {
    }

    public CommentListResponse(List<CommentDTO> comments, Integer total, Integer page, Integer limit) {
        this.comments = comments;
        this.total = total;
        this.page = page;
        this.limit = limit;
    }

    // Getters and Setters
    public List<CommentDTO> getComments() {
        return comments;
    }

    public void setComments(List<CommentDTO> comments) {
        this.comments = comments;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
