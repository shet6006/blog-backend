package com.blog.blog_backend.controller;

import com.blog.blog_backend.service.LikeService;
import com.blog.blog_backend.util.RateLimitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Likes API - 기존 blog와 완전 동일
 */
@RestController
public class LikeController {

    private static final String POST_NOT_FOUND = "게시글을 찾을 수 없습니다.";
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9가-힣-]+$");
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Autowired
    private LikeService likeService;

    /** GET /api/posts/{slug}/likes?deviceId= */
    @GetMapping("/api/posts/{slug}/likes")
    public ResponseEntity<?> getLikeStatus(
            @PathVariable String slug,
            @RequestParam String deviceId,
            HttpServletRequest request) {
        if (!RateLimitUtil.allowLikesGet(RateLimitUtil.getClientIdentifier(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "요청이 너무 많습니다."));
        }
        if (!validateSlug(slug) || !validateDeviceId(deviceId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "잘못된 요청입니다."));
        }
        try {
            return ResponseEntity.ok(likeService.getLikeStatus(slug, deviceId));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/posts/{slug}/likes */
    @PostMapping("/api/posts/{slug}/likes")
    public ResponseEntity<?> toggleLike(
            @PathVariable String slug,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        if (!RateLimitUtil.allowLikesPost(RateLimitUtil.getClientIdentifier(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        if (!validateSlug(slug)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "잘못된 요청입니다."));
        }
        String deviceId = body != null ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "deviceId가 필요합니다."));
        }
        if (!validateDeviceId(deviceId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "잘못된 deviceId입니다."));
        }
        try {
            return ResponseEntity.ok(likeService.toggleLike(slug, deviceId));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/likes?postId=&deviceId= */
    @GetMapping("/api/likes")
    public ResponseEntity<?> getLikeStatusByPostId(
            @RequestParam long postId,
            @RequestParam String deviceId) {
        try {
            return ResponseEntity.ok(likeService.getLikeStatusByPostId(postId, deviceId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch like status"));
        }
    }

    /** POST /api/likes body {post_id, device_id} */
    @PostMapping("/api/likes")
    public ResponseEntity<?> toggleLikeByPostId(@RequestBody Map<String, Object> body) {
        Object postIdObj = body != null ? body.get("post_id") : null;
        Object deviceIdObj = body != null ? body.get("device_id") : null;
        if (postIdObj == null || deviceIdObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing required fields"));
        }
        long postId = postIdObj instanceof Number ? ((Number) postIdObj).longValue() : Long.parseLong(postIdObj.toString());
        String deviceId = deviceIdObj.toString();
        try {
            return ResponseEntity.ok(likeService.toggleLikeByPostId(postId, deviceId));
        } catch (RuntimeException e) {
            if (POST_NOT_FOUND.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to toggle like"));
        }
    }

    private boolean validateSlug(String slug) {
        return slug != null && SLUG_PATTERN.matcher(slug).matches() && slug.length() <= 255;
    }

    private boolean validateDeviceId(String deviceId) {
        return deviceId != null && DEVICE_ID_PATTERN.matcher(deviceId).matches() && deviceId.length() <= 100;
    }
}
