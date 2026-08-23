package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** GET /api/admin/posts 관리자 목록 항목. */
public class AdminPostSummaryResponse {

    @JsonProperty("id")
    private final Long id;
    @JsonProperty("title")
    private final String title;
    @JsonProperty("category_name")
    private final String categoryName;
    @JsonProperty("slug")
    private final String slug;
    @JsonProperty("is_public")
    private final Boolean isPublic;
    @JsonProperty("likes_count")
    private final Integer likesCount;
    @JsonProperty("comments_count")
    private final Integer commentsCount;
    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public AdminPostSummaryResponse(
            Long id,
            String title,
            String categoryName,
            String slug,
            Boolean isPublic,
            Integer likesCount,
            Integer commentsCount,
            LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.categoryName = categoryName;
        this.slug = slug;
        this.isPublic = isPublic;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategoryName() { return categoryName; }
    public String getSlug() { return slug; }
    public Boolean getIsPublic() { return isPublic; }
    public Integer getLikesCount() { return likesCount; }
    public Integer getCommentsCount() { return commentsCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
