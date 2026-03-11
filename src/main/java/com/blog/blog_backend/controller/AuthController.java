package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.entity.AdminProfile;
import com.blog.blog_backend.repository.AdminRepository;
import com.blog.blog_backend.util.AuthUtil;
import com.blog.blog_backend.util.RateLimitUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Auth API - 기존 blog와 완전 동일
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.jwt-secret:}")
    private String jwtSecret;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AuthUtil authUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String clientId = RateLimitUtil.getClientIdentifier(request);
        if (!RateLimitUtil.allowLogin(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "로그인 시도가 너무 많습니다. 1분 후 다시 시도해주세요."));
        }

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "아이디와 비밀번호를 입력해주세요."));
        }
        if (username.length() > 50 || password.length() > 100) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "잘못된 입력입니다."));
        }

        Optional<AdminProfile> adminOpt = adminRepository.findById(username);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "존재하지 않는 아이디입니다."));
        }

        AdminProfile admin = adminOpt.get();
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "비밀번호가 일치하지 않습니다."));
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 설정 오류가 발생했습니다."));
        }

        String token = createToken(admin.getId(), admin.getName());
        setTokenCookie(response, token, 3600);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "로그인 성공");
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", admin.getId() != null ? admin.getId() : "");
        userMap.put("name", admin.getName() != null ? admin.getName() : "");
        result.put("user", userMap);
        return ResponseEntity.ok(result);
    }

    /** POST /api/auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
    }

    /** GET /api/auth/check */
    @GetMapping("/check")
    public ResponseEntity<?> check(HttpServletRequest request) {
        String userId = authUtil.getUserIdFromToken(request);
        if (userId == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("authenticated", false);
            res.put("user", null);
            return ResponseEntity.ok(res);
        }

        Optional<AdminProfile> adminOpt = adminRepository.findById(userId);
        if (adminOpt.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("authenticated", false);
            res.put("user", null);
            return ResponseEntity.ok(res);
        }

        AdminProfile admin = adminOpt.get();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", admin.getId() != null ? admin.getId() : "");
        userMap.put("name", admin.getName() != null ? admin.getName() : "");
        Map<String, Object> res = new HashMap<>();
        res.put("authenticated", true);
        res.put("user", userMap);
        return ResponseEntity.ok(res);
    }

    private String createToken(String userId, String name) {
        SecretKey key = AuthUtil.deriveKey(jwtSecret);
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    /** 크로스오리진(3000→8080)에서 쿠키 전달을 위해 SameSite=None; Secure 필수 */
    private void setTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .path("/")
                .httpOnly(true)
                .secure(true)  // SameSite=None 시 필수, localhost는 secure context로 동작
                .sameSite("None")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
