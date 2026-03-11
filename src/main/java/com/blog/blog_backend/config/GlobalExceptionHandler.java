package com.blog.blog_backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 예상치 못한 예외를 잡아 JSON 형식으로 반환. 500 디버깅 시 콘솔 로그 확인.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        ex.printStackTrace(); // 서버 콘솔에 스택트레이스 출력
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "서버 오류가 발생했습니다.",
                        "message", ex.getMessage() != null ? ex.getMessage() : "알 수 없는 오류"));
    }
}
