package com.example.ucmconnectapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response wrapper for paginated list of posts
 */
@Schema(description = "Post list response with pagination")
public class PostListResponse {

    @Schema(description = "List of posts")
    private List<PostDTO> posts;

    @Schema(description = "Total number of posts", example = "150")
    private Integer total;

    @Schema(description = "Current page number", example = "1")
    private Integer page;

    @Schema(description = "Items per page", example = "20")
    private Integer limit;

    // Constructors
    public PostListResponse() {
    }

    public PostListResponse(List<PostDTO> posts, Integer total, Integer page, Integer limit) {
        this.posts = posts;
        this.total = total;
        this.page = page;
        this.limit = limit;
    }

    // Getters and Setters
    public List<PostDTO> getPosts() {
        return posts;
    }

    public void setPosts(List<PostDTO> posts) {
        this.posts = posts;
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
