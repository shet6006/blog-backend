package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class CategoryResponse {
    @JsonProperty("id")
    private final Long id;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("slug")
    private final String slug;

    @JsonProperty("post_count")
    private final Long postCount;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    public CategoryResponse(
            Long id,
            String name,
            String slug,
            Long postCount,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.postCount = postCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Long getPostCount() { return postCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
