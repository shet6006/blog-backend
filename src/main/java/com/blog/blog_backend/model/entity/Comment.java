package com.blog.blog_backend.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "is_admin")
    private Boolean isAdmin;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
