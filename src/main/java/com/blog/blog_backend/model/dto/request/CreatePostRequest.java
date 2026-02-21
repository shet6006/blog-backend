package com.blog.blog_backend.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST /api/posts 요청 body (기존 blog: title, content, category_id, github_commit_url, is_public)
 */
public class CreatePostRequest {

    private String title;
    private String content;
    @JsonProperty("category_id")
    private Integer categoryId;
    @JsonProperty("github_commit_url")
    private String githubCommitUrl;
    @JsonProperty("is_public")
    private Boolean isPublic;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getGithubCommitUrl() { return githubCommitUrl; }
    public void setGithubCommitUrl(String githubCommitUrl) { this.githubCommitUrl = githubCommitUrl; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
