package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /** slug로 게시글 조회 (댓글 API 등에서 사용) */
    Optional<Post> findBySlug(String slug);

    /** 공개 게시글 수 (기존 Next.js: SELECT COUNT(*) FROM posts WHERE is_public = true) */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isPublic = true")
    long countPublicPosts();
}
