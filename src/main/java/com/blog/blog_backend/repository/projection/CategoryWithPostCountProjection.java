package com.blog.blog_backend.repository.projection;

import java.time.LocalDateTime;

public interface CategoryWithPostCountProjection {
    Long getId();
    String getName();
    String getSlug();
    Long getPostCount();
    LocalDateTime getCreatedAt();
}
