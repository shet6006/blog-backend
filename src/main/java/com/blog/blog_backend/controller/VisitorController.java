package com.blog.blog_backend.controller;

import com.blog.blog_backend.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.blog.blog_backend.util.RateLimitUtil;
import com.blog.blog_backend.model.dto.response.VisitorTrackResponse;
@RestController
@RequestMapping("/api/visitors/track")
public class VisitorController {
    @Autowired
    private VisitorService visitorService;

    @PostMapping
    public ResponseEntity<?> trackVisitor(HttpServletRequest request) {
        String clientId = RateLimitUtil.getClientIdentifier(request);
        try {
            VisitorTrackResponse response = visitorService.track(clientId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorBody("방문자 집계 중 오류가 발생했습니다."));
        }
    }

    private static class ErrorBody {
        private String error;
        public ErrorBody(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
