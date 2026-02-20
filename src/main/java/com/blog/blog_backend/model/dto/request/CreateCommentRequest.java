package com.blog.blog_backend.model.dto.request;

/**
 * POST /api/comments/{slug} 요청 body (기존 blog: authorName, content, deviceId)
 */
public class CreateCommentRequest {

    private String authorName;
    private String content;
    private String deviceId;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
