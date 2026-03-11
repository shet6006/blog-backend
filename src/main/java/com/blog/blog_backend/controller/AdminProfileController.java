package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.entity.AdminProfile;
import com.blog.blog_backend.repository.AdminRepository;
import com.blog.blog_backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GET/PUT /api/admin/profile - 기존 blog와 완전 동일
 */
@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AuthUtil authUtil;

    /** GET - 비인증이면 첫 번째 관리자, 인증이면 본인 프로필 */
    @GetMapping
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String userId = authUtil.getUserIdFromToken(request);

        if (userId == null) {
            List<AdminProfile> list = adminRepository.findAll();
            if (list.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "프로필을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(Map.of("profile", toProfileMap(list.get(0))));
        }

        Optional<AdminProfile> adminOpt = adminRepository.findById(userId);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "프로필을 찾을 수 없습니다."));
        }
        return ResponseEntity.ok(Map.of("profile", toProfileMap(adminOpt.get())));
    }

    /** PUT - 인증 필수 */
    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String userId = authUtil.getUserIdFromToken(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }

        Optional<AdminProfile> adminOpt = adminRepository.findById(userId);
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "프로필을 찾을 수 없습니다."));
        }

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "이름은 필수입니다."));
        }

        AdminProfile admin = adminOpt.get();
        admin.setName(name);
        admin.setEmail(body.get("email"));
        admin.setAvatarUrl(body.get("avatar_url"));
        admin.setGithubUsername(body.get("github_username"));
        admin.setBio(body.get("bio"));

        AdminProfile updated = adminRepository.save(admin);
        return ResponseEntity.ok(Map.of("profile", toProfileMap(updated)));
    }

    private Map<String, Object> toProfileMap(AdminProfile a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("email", a.getEmail());
        m.put("avatar_url", a.getAvatarUrl());
        m.put("github_username", a.getGithubUsername());
        m.put("bio", a.getBio());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }
}
