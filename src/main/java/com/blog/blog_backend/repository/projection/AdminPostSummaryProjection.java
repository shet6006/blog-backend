package com.blog.blog_backend.repository.projection;

import java.time.LocalDateTime;

public interface AdminPostSummaryProjection {
    Long getId();
    String getTitle();
    String getCategoryName();
    String getSlug();
    Boolean getIsPublic();
    Integer getLikesCount();
    Integer getCommentsCount();
    LocalDateTime getCreatedAt();
}
