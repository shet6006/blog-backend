package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.dto.request.CreatePostRequest;
import com.blog.blog_backend.model.dto.request.UpdatePostRequest;
import com.blog.blog_backend.model.dto.response.PostListResponse;
import com.blog.blog_backend.model.dto.response.PostResponse;
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
 * Post API - 기존 blog와 완전 동일
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final String POST_NOT_FOUND = "게시글을 찾을 수 없습니다.";
    private static final String NO_PERMISSION = "권한이 없습니다.";

    @Autowired
    private PostService postService;
    @Autowired
    private AuthUtil authUtil;

    /** GET /api/posts */
    @GetMapping
    public ResponseEntity<?> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean includePrivate,
            @RequestParam(defaultValue = "created_at") String sortBy) {
        try {
            PostListResponse result = postService.findAll(
                    includePrivate, category, search, page, limit, sortBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch posts"));
        }
    }

    /** GET /api/posts/{slug} - 관리자면 비공개 글도 조회 */
    @GetMapping("/{slug}")
    public ResponseEntity<?> getPostBySlug(@PathVariable String slug, HttpServletRequest request) {
        boolean includePrivate = authUtil.isAuthenticatedAdmin(request);
        try {
            PostResponse post = postService.getBySlug(slug, includePrivate);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류"));
        }
    }

    /** POST /api/posts - Rate limit 5/min, authorId는 JWT에서 (없으면 admin) */
    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {
        String clientId = RateLimitUtil.getClientIdentifier(httpRequest);
        if (!RateLimitUtil.allowPostsPost(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        try {
            String authorId = authUtil.getUserIdFromToken(httpRequest);
            PostResponse created = postService.create(request, authorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            if ("Missing required fields".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing required fields"));
            }
            if ("잘못된 카테고리입니다.".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
            }
            if ("제목이 너무 깁니다.".equals(e.getMessage()) || "내용이 너무 깁니다.".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create post"));
        }
    }

    /** PUT /api/posts/{slug} - 관리자만 */
    @PutMapping("/{slug}")
    public ResponseEntity<?> updatePost(
            @PathVariable String slug,
            @RequestBody UpdatePostRequest request,
            HttpServletRequest httpRequest) {
        if (!authUtil.isAuthenticatedAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", NO_PERMISSION));
        }
        try {
            PostResponse updated = postService.update(slug, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            if ("잘못된 카테고리입니다.".equals(e.getMessage()) || "이미 사용 중인 slug입니다.".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류"));
        }
    }

    /** DELETE /api/posts/{slug} - 관리자만 */
    @DeleteMapping("/{slug}")
    public ResponseEntity<?> deletePost(@PathVariable String slug, HttpServletRequest httpRequest) {
        if (!authUtil.isAuthenticatedAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", NO_PERMISSION));
        }
        try {
            postService.delete(slug);
            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류"));
        }
    }
}
