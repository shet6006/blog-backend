package com.blog.blog_backend.controller;

import com.blog.blog_backend.service.PostService;
import com.blog.blog_backend.util.AuthUtil;
import com.blog.blog_backend.util.RateLimitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * GET/POST /api/admin/posts - 기존 blog와 완전 동일 (관리자용)
 * GET: 모든 게시글 (includePrivate=true와 동일)
 * POST: 게시글 생성 (관리자 인증 필수)
 */
@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostsController {

    @Autowired
    private PostService postService;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<?> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }
        try {
            return ResponseEntity.ok(postService.findAll(true, null, null, page, limit, "created_at"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 목록 조회 중 오류가 발생했습니다."));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        if (!RateLimitUtil.allowPostsPost(RateLimitUtil.getClientIdentifier(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }

        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Object catObj = body.get("category_id");
        Object isPublicObj = body.get("is_public");

        if (title == null || title.isBlank() || content == null || content.isBlank() || catObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "제목, 내용, 카테고리는 필수 입력 항목입니다."));
        }
        int categoryId = catObj instanceof Number ? ((Number) catObj).intValue() : Integer.parseInt(catObj.toString());
        boolean isPublic = isPublicObj == null || Boolean.TRUE.equals(isPublicObj);

        com.blog.blog_backend.model.dto.request.CreatePostRequest req = new com.blog.blog_backend.model.dto.request.CreatePostRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setCategoryId(categoryId);
        req.setIsPublic(isPublic);
        String authorId = authUtil.getUserIdFromToken(request);

        try {
            return ResponseEntity.ok(postService.create(req, authorId));
        } catch (RuntimeException e) {
            if ("이미 사용 중인 슬러그입니다.".equals(e.getMessage()) || "이미 사용 중인 slug입니다.".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 생성 중 오류가 발생했습니다."));
        }
    }
}
