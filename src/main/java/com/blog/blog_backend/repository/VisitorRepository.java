package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    /** 중복 제거 방문자 수 (기존 Next.js: SELECT COUNT(DISTINCT ip_address) FROM visitors) */
    @Query("SELECT COUNT(DISTINCT v.ipAddress) FROM Visitor v")
    long countDistinctByIpAddress();
}
