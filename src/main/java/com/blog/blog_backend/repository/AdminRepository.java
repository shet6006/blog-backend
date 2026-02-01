package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.dto.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<AdminProfile, String> {
    // JpaRepository가 기본 CRUD 메서드 자동 제공:
    // - findById(String id) -> Optional<AdminProfile>  (이것만 사용하면 됨)
    // - save(AdminProfile admin) -> AdminProfile
    // - deleteById(String id) -> void
    // - findAll() -> List<AdminProfile>
    // - existsById(String id) -> boolean

    // 기존 백엔드와 동일하게 id(username)로만 조회하므로 추가 메서드 불필요
}