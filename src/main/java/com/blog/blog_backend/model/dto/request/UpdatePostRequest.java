package com.blog.blog_backend.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PUT /api/posts/{slug} 요청 body (기존 blog: title, content, category_id, is_public, slug)
 */
public class UpdatePostRequest {

    private String title;
    private String content;
    @JsonProperty("category_id")
    private Integer categoryId;
    @JsonProperty("is_public")
    private Boolean isPublic;
    private String slug;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
