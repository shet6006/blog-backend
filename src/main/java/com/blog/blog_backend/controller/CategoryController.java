package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.dto.response.CategoryResponse;
import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // GET /api/categories - 카테고리 목록 조회 (기존 백엔드와 동일)
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<CategoryResponse> categories = categoryService.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch categories"));
        }
    }

    // POST /api/categories - 카테고리 생성 (기존 백엔드와 동일)
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Category name is required"));
            }

            CategoryResponse newCategory = categoryService.create(name);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCategory);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create category"));
        }
    }

    // GET /api/categories/{slug} - slug로 카테고리 조회 (기존 백엔드와 동일)
    @GetMapping("/{slug}")
    public ResponseEntity<?> getCategoryBySlug(@PathVariable String slug) {
        try {
            CategoryResponse category = categoryService.getBySlug(slug);
            return ResponseEntity.ok(category);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("카테고리를 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류"));
        }
    }

    // PUT /api/categories/{slug} - slug로 카테고리 수정 (기존 백엔드와 동일)
    @PutMapping("/{slug}")
    public ResponseEntity<?> updateCategoryBySlug(@PathVariable String slug, @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            if (name == null || name.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Category name is required"));
            }
            CategoryResponse updated = categoryService.updateBySlug(slug, name);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("카테고리를 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류"));
        }
    }

    // DELETE /api/categories/{slug} - slug로 카테고리 삭제 (기존 백엔드와 동일)
    @DeleteMapping("/{slug}")
    public ResponseEntity<?> deleteCategoryBySlug(@PathVariable String slug) {
        try {
            categoryService.deleteBySlug(slug);
            return ResponseEntity.ok(new MessageResponse("카테고리가 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("카테고리를 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류"));
        }
    }

    // 에러 응답 클래스
    private static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // 메시지 응답 클래스
    private static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
