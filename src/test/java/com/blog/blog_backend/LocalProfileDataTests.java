package com.blog.blog_backend;

import com.blog.blog_backend.model.entity.AdminProfile;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.AdminRepository;
import com.blog.blog_backend.repository.PostRepository;
import com.blog.blog_backend.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class LocalProfileDataTests {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryService categoryService;

    @Test
    void initializesLocalAdminAndSamplePost() {
        AdminProfile admin = adminRepository.findById("admin").orElseThrow();
        Post post = postRepository.findBySlug("deployment-and-cicd-pipeline").orElseThrow();

        assertThat(admin.getName()).isEqualTo("김동원");
        assertThat(admin.getGithubUsername()).isEqualTo("DDONG");
        assertThat(new BCryptPasswordEncoder().matches("admin1234", admin.getPassword())).isTrue();
        assertThat(post.getAuthorId()).isEqualTo(admin.getId());
        assertThat(post.getIsPublic()).isTrue();
        assertThat(postRepository.count()).isEqualTo(3);
    }

    @Test
    void aggregatesPublicPostCountsWithCategories() {
        long categoryPostCount = categoryService.findAll().stream()
                .mapToLong(category -> category.getPostCount())
                .sum();

        assertThat(categoryPostCount).isEqualTo(postRepository.countPublicPosts());
    }
}
