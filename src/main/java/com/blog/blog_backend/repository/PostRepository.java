package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.projection.AdminPostSummaryProjection;
import com.blog.blog_backend.repository.projection.PublicPostSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(value = """
            SELECT p.id AS id, p.title AS title, p.excerpt AS excerpt,
                   c.name AS categoryName, p.slug AS slug, p.thumbnailUrl AS thumbnailUrl,
                   COALESCE(p.likesCount, 0) AS likesCount,
                   COALESCE(p.commentsCount, 0) AS commentsCount, p.createdAt AS createdAt
            FROM Post p
            LEFT JOIN p.category c
            WHERE p.isPublic = true
              AND (:category = '' OR c.name = :category)
              AND (:filterSearch = false OR p.title LIKE CONCAT('%', :search, '%')
                   OR p.content LIKE CONCAT('%', :search, '%')
                   OR c.name LIKE CONCAT('%', :search, '%'))
            """,
            countQuery = """
            SELECT COUNT(p)
            FROM Post p
            LEFT JOIN p.category c
            WHERE p.isPublic = true
              AND (:category = '' OR c.name = :category)
              AND (:filterSearch = false OR p.title LIKE CONCAT('%', :search, '%')
                   OR p.content LIKE CONCAT('%', :search, '%')
                   OR c.name LIKE CONCAT('%', :search, '%'))
            """)
    Page<PublicPostSummaryProjection> findPublicSummaries(
            String category,
            boolean filterSearch,
            String search,
            Pageable pageable);

    @Query(value = """
            SELECT p.id AS id, p.title AS title, c.name AS categoryName, p.slug AS slug,
                   p.isPublic AS isPublic, COALESCE(p.likesCount, 0) AS likesCount,
                   COALESCE(p.commentsCount, 0) AS commentsCount, p.createdAt AS createdAt
            FROM Post p
            LEFT JOIN p.category c
            """,
            countQuery = "SELECT COUNT(p) FROM Post p")
    Page<AdminPostSummaryProjection> findAdminSummaries(Pageable pageable);

    /** slug로 게시글 조회 (댓글 API 등에서 사용) */
    Optional<Post> findBySlug(String slug);

    /** 공개 게시글 수 (기존 Next.js: SELECT COUNT(*) FROM posts WHERE is_public = true) */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isPublic = true")
    long countPublicPosts();

    /** 해당 카테고리를 사용하는 게시글 수 */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category.id = :categoryId")
    long countByCategoryId(Long categoryId);
}
