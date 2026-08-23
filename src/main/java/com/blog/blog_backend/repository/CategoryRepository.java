package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.repository.projection.CategoryWithPostCountProjection;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByName(String name);

    default List<Category> findAllByOrderByCreatedAtDesc() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Query("""
            SELECT c.id AS id, c.name AS name, c.slug AS slug,
                   COUNT(p.id) AS postCount, c.createdAt AS createdAt
            FROM Category c
            LEFT JOIN Post p ON p.category = c AND p.isPublic = true
            GROUP BY c.id, c.name, c.slug, c.createdAt
            ORDER BY c.createdAt DESC
            """)
    List<CategoryWithPostCountProjection> findAllWithPostCount();

    // 게시글 수 조회
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId AND p.isPublic = true")
    Long countPostsByCategoryId(Long categoryId);
}
