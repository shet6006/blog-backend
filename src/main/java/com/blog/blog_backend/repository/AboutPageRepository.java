package com.blog.blog_backend.repository;

import com.blog.blog_backend.model.entity.AboutPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AboutPageRepository extends JpaRepository<AboutPage, Long> {

    Optional<AboutPage> findTopByOrderByIdDesc();
}
