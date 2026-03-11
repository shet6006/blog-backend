package com.blog.blog_backend.controller;

import com.blog.blog_backend.service.AboutService;
import com.blog.blog_backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * GET/PUT /api/about - 기존 blog와 완전 동일
 */
@RestController
@RequestMapping("/api/about")
public class AboutController {

    @Autowired
    private AboutService aboutService;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<?> getAbout() {
        return ResponseEntity.ok(aboutService.getAbout());
    }

    @PutMapping
    public ResponseEntity<?> updateAbout(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }

        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Object techStack = body.get("tech_stack");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "제목과 내용은 필수입니다."));
        }

        try {
            return ResponseEntity.ok(aboutService.updateAbout(title, content, techStack));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "소개 페이지 업데이트에 실패했습니다."));
        }
    }
}
