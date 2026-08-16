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
                .name("김동원")
                .email("admin@localhost")
                .password(passwordEncoder.encode("admin1234"))
                .githubUsername("DDONG")
                .bio("안녕하세요. 신입 백엔드 개발자 김동원입니다.")
                .build();
        adminRepository.save(admin);

        Category til = createCategory("TIL", "til");
        Category gitAndGithub = createCategory("Git & Github", "git-github");

        createPost(
                "251126 TIL",
                "251126-til",
                "첫 배포 성공과 CloudFront 캐시 문제를 해결한 기록입니다.",
                """
                ## 첫 배포 성공, 그리고 도메인

                블로그 프로젝트를 AWS EC2를 활용해 배포하는 데 성공했다. 하지만 매번 IP 주소로 접속하는 것이 사용자 경험 측면에서 아쉽게 느껴졌다.

                ## CloudFront 캐시 불일치

                새 코드를 배포한 뒤 구 버전 HTML이 새로운 JavaScript 청크를 찾지 못해 화면이 깨졌다. CloudFront 무효화를 실행해 최신 정적 파일이 전달되도록 하면서 문제를 해결했다.

                이 경험을 통해 CDN 환경에서는 배포뿐 아니라 캐시 관리도 함께 고려해야 한다는 점을 배웠다.
                """,
                til,
                admin);

        createPost(
                "251129 TIL",
                "251129-til",
                "새 기기에서 개발 환경을 구성하며 배운 프로젝트 관리와 인증 이야기입니다.",
                """
                ## What I did

                프로젝트를 새로운 기기에 클론한 뒤 Node.js와 MySQL, 환경 변수와 Git에서 관리하지 않는 파일들을 다시 정리했다.

                ## What I learned

                HttpOnly 쿠키는 클라이언트 JavaScript에서 직접 읽을 수 없기 때문에 인증 상태를 서버 API로 확인해야 한다. 또한 데이터베이스 쿼리에서 사용자 ID를 하드코딩하지 않고 JWT 같은 인증 정보에서 동적으로 식별하도록 개선했다.
                """,
                til,
                admin);

        createPost(
                "배포 및 CI/CD 파이프라인 개선",
                "deployment-and-cicd-pipeline",
                "GitHub Actions와 AWS를 이용해 배포 구조를 개선하며 고민한 내용을 정리했습니다.",
                """
                ## 기존 배포 방식의 문제

                GitHub Hosted Runner에서 이미지를 빌드하고 EC2의 Self Hosted Runner가 이미지를 받아 실행하는 구조를 사용했다. 하지만 작은 EC2에서는 러너가 메모리를 많이 사용했고 SSH 접근 관리도 번거로웠다.

                ## 개선 방향

                이미지는 GitHub Actions에서 빌드해 ECR에 저장하고, CodeDeploy를 통해 EC2가 새 이미지를 받아 컨테이너를 교체하도록 변경했다. 프론트엔드는 S3와 CloudFront로 분리해 정적 파일을 제공한다.

                도구를 단순히 사용하는 것보다 현재 인스턴스 환경과 비용, 보안, 롤백 방법을 함께 고려하는 과정이 중요했다.
                """,
                gitAndGithub,
                admin);
    }

    private Category createCategory(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    private void createPost(
            String title,
            String slug,
            String excerpt,
            String content,
            Category category,
            AdminProfile admin) {
        Post post = new Post();
        post.setTitle(title);
        post.setSlug(slug);
        post.setExcerpt(excerpt);
        post.setContent(content);
        post.setCategory(category);
        post.setAuthorId(admin.getId());
        post.setIsPublic(true);
        postRepository.save(post);
    }
}
