package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** GET /api/admin/posts 응답. */
public class AdminPostListResponse {

    @JsonProperty("posts")
    private final List<AdminPostSummaryResponse> posts;
    @JsonProperty("pagination")
    private final PostListResponse.PaginationInfo pagination;

    public AdminPostListResponse(
            List<AdminPostSummaryResponse> posts,
            PostListResponse.PaginationInfo pagination) {
        this.posts = posts;
        this.pagination = pagination;
    }

    public List<AdminPostSummaryResponse> getPosts() { return posts; }
    public PostListResponse.PaginationInfo getPagination() { return pagination; }
}
