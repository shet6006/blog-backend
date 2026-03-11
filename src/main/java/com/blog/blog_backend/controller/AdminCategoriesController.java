package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.repository.CategoryRepository;
import com.blog.blog_backend.repository.PostRepository;
import com.blog.blog_backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GET/POST /api/admin/categories, DELETE /api/admin/categories/{id} - 기존 blog와 완전 동일
 */
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoriesController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<?> getCategories(HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }
        List<Category> list = categoryRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> categories = list.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "slug", c.getSlug(),
                        "created_at", c.getCreatedAt() != null ? c.getCreatedAt() : ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("categories", categories));
    }

    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }
        String name = body.get("name");
        String slug = body.get("slug");
        if (name == null || name.isBlank() || slug == null || slug.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "카테고리 이름과 슬러그는 필수입니다."));
        }
        if (categoryRepository.findByName(name).isPresent() || categoryRepository.findBySlug(slug).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "이미 존재하는 카테고리 이름 또는 슬러그입니다."));
        }
        Category cat = new Category();
        cat.setName(name);
        cat.setSlug(slug);
        Category saved = categoryRepository.save(cat);
        return ResponseEntity.ok(Map.of("category", Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "slug", saved.getSlug(),
                "created_at", saved.getCreatedAt() != null ? saved.getCreatedAt() : "")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Long id,
            HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }
        long postsWithCategory = postRepository.countByCategoryId(id);
        if (postsWithCategory > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "이 카테고리를 사용하는 게시글이 있어 삭제할 수 없습니다."));
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "카테고리가 삭제되었습니다."));
    }
}
