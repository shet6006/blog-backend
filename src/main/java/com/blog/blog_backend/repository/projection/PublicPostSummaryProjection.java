package com.blog.blog_backend.repository.projection;

import java.time.LocalDateTime;

public interface PublicPostSummaryProjection {
    Long getId();
    String getTitle();
    String getExcerpt();
    String getCategoryName();
    String getSlug();
    String getThumbnailUrl();
    Integer getLikesCount();
    Integer getCommentsCount();
    LocalDateTime getCreatedAt();
}
