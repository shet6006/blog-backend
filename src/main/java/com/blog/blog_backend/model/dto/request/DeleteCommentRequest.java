package com.blog.blog_backend.model.dto.request;

/**
 * DELETE /api/comments/{slug} 요청 body.
 * commentId가 있으면 해당 댓글만 삭제, 없으면 해당 글의 댓글 전부 삭제.
 */
public class DeleteCommentRequest {

    private Long commentId;

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
}
