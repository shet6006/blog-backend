package com.blog.blog_backend.config;

import com.blog.blog_backend.model.entity.AdminProfile;
import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.AdminRepository;
import com.blog.blog_backend.repository.CategoryRepository;
import com.blog.blog_backend.repository.PostRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("local")
public class LocalDataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LocalDataInitializer(
            AdminRepository adminRepository,
            CategoryRepository categoryRepository,
            PostRepository postRepository) {
        this.adminRepository = adminRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdminProfile admin = AdminProfile.builder()
                .id("admin")
                .name("Local Admin")
                .email("admin@localhost")
                .password(passwordEncoder.encode("admin1234"))
                .bio("로컬 개발용 관리자 계정입니다.")
                .build();
        adminRepository.save(admin);

        Category category = new Category();
        category.setName("개발");
        category.setSlug("development");
        category.setCreatedAt(LocalDateTime.now());
        categoryRepository.save(category);

        Post post = new Post();
        post.setTitle("로컬 개발환경 샘플 글");
        post.setSlug("local-development-sample");
        post.setExcerpt("로컬 프로필에서 매번 새로 생성되는 테스트 게시글입니다.");
        post.setContent("# 로컬 개발환경\n\n이 글은 backend를 `local` 프로필로 실행할 때 자동으로 생성됩니다.");
        post.setCategory(category);
        post.setAuthorId(admin.getId());
        post.setIsPublic(true);
        postRepository.save(post);
    }
}
