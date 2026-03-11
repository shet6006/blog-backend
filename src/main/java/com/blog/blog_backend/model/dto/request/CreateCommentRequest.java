package com.blog.blog_backend.model.dto.request;

/**
 * POST /api/comments/{slug} 요청 body (기존 blog: authorName, content, deviceId)
 * POST /api/comments 요청 body (기존 blog: postId, authorName, content, deviceId)
 */
public class CreateCommentRequest {

    private Long postId;
    private String authorName;
    private String content;
    private String deviceId;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
