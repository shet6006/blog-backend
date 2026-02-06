package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class CategoryResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("slug")
    private String slug;

    @JsonProperty("post_count")
    private Long postCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Long getPostCount() { return postCount; }
    public void setPostCount(Long postCount) { this.postCount = postCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
