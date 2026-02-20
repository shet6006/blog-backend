package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 댓글 조회/삭제 (기존 Next.js comments API와 동일) */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 해당 글의 댓글 목록, created_at 오름차순 (GET /api/comments/{slug}) */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /** 해당 글의 댓글 개수 (comments_count 업데이트용) */
    long countByPostId(Long postId);

    /** 한 댓글만 삭제 - post_id 일치할 때만 (DELETE body에 commentId 있을 때) */
    void deleteByIdAndPostId(Long id, Long postId);

    /** 해당 글의 댓글 전부 삭제 (DELETE body에 commentId 없을 때) */
    void deleteByPostId(Long postId);
}
