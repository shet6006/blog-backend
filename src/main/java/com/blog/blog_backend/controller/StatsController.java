package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.dto.response.StatsResponse;
import com.blog.blog_backend.service.StatsService;
import com.blog.blog_backend.util.RateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/stats (기존 Next.js와 동일: Rate limit 분당 60회, 응답/에러 메시지 동일)
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        String clientId = RateLimitUtil.getClientIdentifier(request);
        if (!RateLimitUtil.allowStatsRequest(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorBody("요청이 너무 많습니다."));
        }

        try {
            StatsResponse stats = statsService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorBody("통계를 불러오는 중 오류가 발생했습니다."));
        }
    }

    private static class ErrorBody {
        private String error;
        public ErrorBody(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
