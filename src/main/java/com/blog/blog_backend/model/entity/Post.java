package com.blog.blog_backend.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "github_commit_url", length = 500)
    private String githubCommitUrl;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "author_id", length = 50)
    private String authorId;

    @Column(name = "likes_count")
    private Integer likesCount;

    @Column(name = "comments_count")
    private Integer commentsCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getter 메서드들
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getSlug() {
        return slug;
    }

    public String getGithubCommitUrl() {
        return githubCommitUrl;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public String getAuthorId() {
        return authorId;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setter 메서드들
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setGithubCommitUrl(String githubCommitUrl) {
        this.githubCommitUrl = githubCommitUrl;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (likesCount == null) {
            likesCount = 0;
        }
        if (commentsCount == null) {
            commentsCount = 0;
        }
        if (isPublic == null) {
            isPublic = true;
        }
        if (authorId == null) {
            authorId = "admin";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
