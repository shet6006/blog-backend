package com.blog.blog_backend.service;

import com.blog.blog_backend.model.dto.response.CategoryResponse;
import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.repository.CategoryRepository;
import com.blog.blog_backend.repository.projection.CategoryWithPostCountProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@SuppressWarnings("null")
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // 모든 카테고리 조회 (게시글 수 포함) - 기존 백엔드와 동일
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllWithPostCount().stream()
                .map(this::toResponse)
                .toList();
    }

    // ID로 카테고리 조회
    public Category findById(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new RuntimeException("카테고리를 찾을 수 없습니다.");
        }
        return category.get();
    }

    // 슬러그로 카테고리 조회
    public Category findBySlug(String slug) {
        Optional<Category> category = categoryRepository.findBySlug(slug);
        if (category.isEmpty()) {
            throw new RuntimeException("카테고리를 찾을 수 없습니다.");
        }
        return category.get();
    }

    // 슬러그로 카테고리 조회 (응답 DTO)
    public CategoryResponse getBySlug(String slug) {
        Category category = findBySlug(slug);
        return toResponse(category);
    }

    // Entity -> CategoryResponse 변환
    private CategoryResponse toResponse(Category category) {
        Long postCount = categoryRepository.countPostsByCategoryId(category.getId());
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                postCount != null ? postCount : 0L,
                category.getCreatedAt());
    }

    private CategoryResponse toResponse(CategoryWithPostCountProjection category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getPostCount(),
                category.getCreatedAt());
    }

    // 카테고리 생성 - 기존 백엔드와 동일한 슬러그 생성 로직
    public CategoryResponse create(String name) {
        // 슬러그 생성 (기존 백엔드와 동일)
        String slug = name
                .toLowerCase()
                .replaceAll("[^a-z0-9가-힣]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        // 중복 확인
        Optional<Category> existingCategory = categoryRepository.findBySlug(slug);
        if (existingCategory.isPresent()) {
            throw new RuntimeException("이미 존재하는 카테고리입니다.");
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    // 슬러그로 카테고리 수정 (기존 백엔드: name만 수정, slug는 유지)
    public CategoryResponse updateBySlug(String slug, String name) {
        Category category = findBySlug(slug);
        category.setName(name);
        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    // ID로 카테고리 삭제
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("카테고리를 찾을 수 없습니다.");
        }
        categoryRepository.deleteById(id);
    }

    // 슬러그로 카테고리 삭제
    public void deleteBySlug(String slug) {
        Category category = findBySlug(slug);
        categoryRepository.deleteById(category.getId());
    }
}
