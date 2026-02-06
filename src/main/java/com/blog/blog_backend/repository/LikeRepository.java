package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

/** 전체 좋아요 수는 JpaRepository의 count() 사용 (기존: SELECT COUNT(*) FROM likes) */
public interface LikeRepository extends JpaRepository<Like, Long> {
}
