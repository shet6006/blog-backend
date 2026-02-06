package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 공개 게시글 수 (기존 Next.js: SELECT COUNT(*) FROM posts WHERE is_public = true) */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.isPublic = true")
    long countPublicPosts();
}
