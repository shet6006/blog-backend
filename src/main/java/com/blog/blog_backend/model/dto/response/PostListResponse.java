package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GET /api/posts 응답 - posts + pagination (기존 blog와 동일)
 */
public class PostListResponse {

    @JsonProperty("posts")
    private List<PostResponse> posts;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    public PostListResponse(List<PostResponse> posts, PaginationInfo pagination) {
        this.posts = posts;
        this.pagination = pagination;
    }

    public List<PostResponse> getPosts() { return posts; }
    public void setPosts(List<PostResponse> posts) { this.posts = posts; }
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

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
