package com.blog.blog_backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * GET /api/comments/{slug} 응답 항목 (기존 blog와 동일한 필드명: snake_case)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("post_id")
    private Long postId;
    @JsonProperty("author_name")
    private String authorName;
    @JsonProperty("content")
    private String content;
    @JsonProperty("device_id")
    private String deviceId;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("is_admin")
    private Boolean isAdmin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
}
