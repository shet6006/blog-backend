package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GET /api/posts 응답 - posts + pagination (기존 blog와 동일)
 */
public class PostListResponse {

    @JsonProperty("posts")
    private List<PostSummaryResponse> posts;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    public PostListResponse(List<PostSummaryResponse> posts, PaginationInfo pagination) {
        this.posts = posts;
        this.pagination = pagination;
    }

    public List<PostSummaryResponse> getPosts() { return posts; }
    public void setPosts(List<PostSummaryResponse> posts) { this.posts = posts; }
    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }

    public static class PaginationInfo {
        @JsonProperty("page")
        private int page;
        @JsonProperty("limit")
        private int limit;
        @JsonProperty("total")
        private long total;
        @JsonProperty("totalPages")
        private int totalPages;

        public PaginationInfo(int page, int limit, long total) {
            this.page = page;
            this.limit = limit;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / limit);
        }
    }
}
