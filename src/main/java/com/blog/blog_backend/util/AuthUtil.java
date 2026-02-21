package com.blog.blog_backend.util;

import com.blog.blog_backend.repository.AdminRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 기존 Next.js와 동일: token 쿠키의 JWT 검증, admin_profile 테이블로 관리자 확인
 */
@Component
public class AuthUtil {

    private final String jwtSecret;
    private final AdminRepository adminRepository;

    @Autowired
    public AuthUtil(
            @Value("${app.jwt-secret:}") String jwtSecret,
            AdminRepository adminRepository) {
        this.jwtSecret = jwtSecret;
        this.adminRepository = adminRepository;
    }

    /** 인증된 관리자인지 확인 (기존 isAuthenticatedAdmin) */
    public boolean isAuthenticatedAdmin(HttpServletRequest request) {
        String userId = getUserIdFromToken(request);
        if (userId == null) return false;
        return adminRepository.existsById(userId);
    }

    /** JWT token에서 userId 추출 (없거나 검증 실패 시 null) */
    public String getUserIdFromToken(HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtSecret == null || jwtSecret.isBlank()) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object userId = claims.get("userId");
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("token".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
