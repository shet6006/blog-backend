package com.blog.blog_backend.model.dto.response;

public class VisitorTrackResponse {

    private boolean success;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
