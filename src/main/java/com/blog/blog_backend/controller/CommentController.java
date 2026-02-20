package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.dto.request.CreateCommentRequest;
import com.blog.blog_backend.model.dto.request.DeleteCommentRequest;
import com.blog.blog_backend.model.dto.response.CommentResponse;
import com.blog.blog_backend.service.CommentService;
import com.blog.blog_backend.util.RateLimitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private static final String POST_NOT_FOUND_MSG = "게시글을 찾을 수 없습니다.";

    @Autowired
    private CommentService commentService;

    /** GET /api/comments/{slug} - 해당 글의 댓글 목록 (created_at ASC) */
    @GetMapping("/{slug}")
    public ResponseEntity<?> getComments(@PathVariable String slug, HttpServletRequest request) {
        String clientId = RateLimitUtil.getClientIdentifier(request);
        if (!RateLimitUtil.allowCommentsGet(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        try {
            List<CommentResponse> list = commentService.getCommentsBySlug(slug);
            return ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류"));
        }
    }

    /** POST /api/comments/{slug} - 댓글 등록 */
    @PostMapping("/{slug}")
    public ResponseEntity<?> createComment(
            @PathVariable String slug,
            @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        String clientId = RateLimitUtil.getClientIdentifier(httpRequest);
        if (!RateLimitUtil.allowCommentsPost(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "댓글 작성이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        try {
            commentService.createComment(slug, request);
            return ResponseEntity.ok(Map.of("message", "댓글이 등록되었습니다."));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND_MSG.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류"));
        }
    }

    /** DELETE /api/comments/{slug} - commentId 있으면 해당 댓글만, 없으면 해당 글 댓글 전부 삭제 */
    @DeleteMapping("/{slug}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String slug,
            @RequestBody(required = false) DeleteCommentRequest body,
            HttpServletRequest request) {
        Long commentId = (body != null) ? body.getCommentId() : null;
        try {
            commentService.deleteComment(slug, commentId);
            return ResponseEntity.ok(Map.of("message", "댓글이 삭제되었습니다."));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND_MSG.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류"));
        }
    }
}
