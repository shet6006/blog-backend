package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByName(String name);

    // 게시글 수를 포함한 카테고리 조회 (기존 백엔드와 동일한 쿼리)
    @Query("SELECT c FROM Category c LEFT JOIN Post p ON c.id = p.category.id AND p.isPublic = true " +
            "GROUP BY c.id ORDER BY c.name")
    List<Category> findAllWithPostCount();

    // 게시글 수 조회
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId AND p.isPublic = true")
    Long countPostsByCategoryId(Long categoryId);
}
