package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** GET /api/posts 목록 항목. 게시글 본문은 상세 API에서만 반환한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostSummaryResponse {

    @JsonProperty("id")
    private final Long id;
    @JsonProperty("title")
    private final String title;
    @JsonProperty("excerpt")
    private final String excerpt;
    @JsonProperty("category_name")
    private final String categoryName;
    @JsonProperty("slug")
    private final String slug;
    @JsonProperty("thumbnail_url")
    private final String thumbnailUrl;
    @JsonProperty("likes_count")
    private final Integer likesCount;
    @JsonProperty("comments_count")
    private final Integer commentsCount;
    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public PostSummaryResponse(
            Long id,
            String title,
            String excerpt,
            String categoryName,
            String slug,
            String thumbnailUrl,
            Integer likesCount,
            Integer commentsCount,
            LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.excerpt = excerpt;
        this.categoryName = categoryName;
        this.slug = slug;
        this.thumbnailUrl = thumbnailUrl;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getExcerpt() { return excerpt; }
    public String getCategoryName() { return categoryName; }
    public String getSlug() { return slug; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Integer getLikesCount() { return likesCount; }
    public Integer getCommentsCount() { return commentsCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
