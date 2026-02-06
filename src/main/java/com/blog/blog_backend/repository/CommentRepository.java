package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/** 전체 댓글 수는 JpaRepository의 count() 사용 (기존: SELECT COUNT(*) FROM comments) */
public interface CommentRepository extends JpaRepository<Comment, Long> {
}
