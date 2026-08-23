package com.blog.blog_backend.service;

import com.blog.blog_backend.model.dto.request.CreatePostRequest;
import com.blog.blog_backend.model.dto.request.UpdatePostRequest;
import com.blog.blog_backend.model.dto.response.AdminPostListResponse;
import com.blog.blog_backend.model.dto.response.AdminPostSummaryResponse;
import com.blog.blog_backend.model.dto.response.PostListResponse;
import com.blog.blog_backend.model.dto.response.PostResponse;
import com.blog.blog_backend.model.dto.response.PostSummaryResponse;
import com.blog.blog_backend.model.entity.Category;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.CategoryRepository;
import com.blog.blog_backend.repository.PostRepository;
import com.blog.blog_backend.repository.projection.AdminPostSummaryProjection;
import com.blog.blog_backend.repository.projection.PublicPostSummaryProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PostService {

    private static final int TITLE_MAX = 255;
    private static final int CONTENT_MAX = 100000;
    private static final int IMAGE_MAX = 20;
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\([^\\)]*\\)");
    private static final Pattern MARKDOWN_IMAGE_URL_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\((?:<)?([^\\s)>]+)");

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * GET /api/posts - 게시글 목록 조회 (기존 Next.js PostModel.findAll과 동일)
     * @param category 카테고리 이름, null/"All"이면 전체
     * @param search 검색어 (제목/내용/카테고리명)
     * @param page 1-based
     * @param limit 페이지당 개수
     * @param sortBy "likes" 또는 "created_at"
     */
    @Transactional(readOnly = true)
    public PostListResponse findPublicPosts(String category, String search,
                                            int page, int limit, String sortBy) {
        // 페이지네이션 제한 (기존 Next.js와 동일)
        int validatedPage = page < 1 ? 1 : Math.min(page, 1000);
        int validatedLimit = limit < 1 ? 10 : Math.min(limit, 100);
        final String searchTerm = (search != null && search.length() > 100) ? search.substring(0, 100) : search;

        Sort sort = "likes".equals(sortBy)
                ? Sort.by(Sort.Direction.DESC, "likesCount")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, sort);

        boolean filterSearch = searchTerm != null && !searchTerm.isBlank();
        String categoryFilter = category != null && !category.isBlank() && !"All".equals(category)
                ? category : "";
        Page<PublicPostSummaryProjection> postPage = postRepository.findPublicSummaries(
                categoryFilter,
                filterSearch, filterSearch ? searchTerm : "",
                pageable);
        List<PostSummaryResponse> posts = postPage.getContent().stream()
                .map(this::toPostSummaryResponse)
                .toList();

        var pagination = new PostListResponse.PaginationInfo(
                validatedPage, validatedLimit, postPage.getTotalElements());

        return new PostListResponse(posts, pagination);
    }

    @Transactional(readOnly = true)
    public AdminPostListResponse findAdminPosts(int page, int limit) {
        int validatedPage = page < 1 ? 1 : Math.min(page, 1000);
        int validatedLimit = limit < 1 ? 10 : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(
                validatedPage - 1,
                validatedLimit,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AdminPostSummaryProjection> postPage = postRepository.findAdminSummaries(pageable);
        List<AdminPostSummaryResponse> posts = postPage.getContent().stream()
                .map(this::toAdminPostSummaryResponse)
                .toList();
        var pagination = new PostListResponse.PaginationInfo(
                validatedPage, validatedLimit, postPage.getTotalElements());

        return new AdminPostListResponse(posts, pagination);
    }

    /**
     * GET /api/posts/{slug} - slug로 게시글 상세 조회
     * @param includePrivate true면 비공개 글도 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public PostResponse getBySlug(String slug, boolean includePrivate) {
        Optional<Post> opt = postRepository.findBySlug(slug);
        if (opt.isEmpty()) {
            throw new RuntimeException("게시글을 찾을 수 없습니다.");
        }
        Post post = opt.get();
        if (!includePrivate && !Boolean.TRUE.equals(post.getIsPublic())) {
            throw new RuntimeException("게시글을 찾을 수 없습니다.");
        }
        return toResponse(post);
    }

    /**
     * POST /api/posts - 게시글 생성 (기존 Next.js와 동일)
     */
    @Transactional
    public PostResponse create(CreatePostRequest req, String authorId) {
        validateCreateRequest(req);
        Category category = categoryRepository.findById(req.getCategoryId().longValue())
                .orElseThrow(() -> new RuntimeException("잘못된 카테고리입니다."));

        String slug = generateSlugFromTitle(req.getTitle());
        if (postRepository.findBySlug(slug).isPresent()) {
            throw new RuntimeException("이미 사용 중인 slug입니다.");
        }
        String excerpt = generateExcerpt(req.getContent());

        Post post = new Post();
        post.setTitle(req.getTitle().trim());
        post.setContent(req.getContent());
        post.setExcerpt(excerpt);
        post.setCategory(category);
        post.setSlug(slug);
        post.setGithubCommitUrl(req.getGithubCommitUrl() != null && !req.getGithubCommitUrl().isBlank()
                ? req.getGithubCommitUrl().trim() : null);
        post.setThumbnailUrl(resolveThumbnailUrl(req.getThumbnailUrl(), req.getContent()));
        post.setIsPublic(req.getIsPublic() != null ? req.getIsPublic() : true);
        post.setAuthorId(authorId != null ? authorId : "admin");

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    /**
     * PUT /api/posts/{slug} - 게시글 수정 (기존 Next.js와 동일)
     */
    @Transactional
    public PostResponse update(String slug, UpdatePostRequest req) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (req.getTitle() != null) post.setTitle(req.getTitle().trim());
        if (req.getContent() != null) {
            validateImageCount(req.getContent());
            post.setContent(req.getContent());
        }
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId().longValue())
                    .orElseThrow(() -> new RuntimeException("잘못된 카테고리입니다."));
            post.setCategory(category);
        }
        if (req.getIsPublic() != null) post.setIsPublic(req.getIsPublic());
        post.setThumbnailUrl(resolveThumbnailUrl(req.getThumbnailUrl(), post.getContent()));

        String newSlug = req.getSlug() != null && !req.getSlug().isBlank() ? req.getSlug().trim() : slug;
        if (!newSlug.equals(slug)) {
            if (postRepository.findBySlug(newSlug).isPresent()) {
                throw new RuntimeException("이미 사용 중인 slug입니다.");
            }
            post.setSlug(newSlug);
        }

        post.setExcerpt(generateExcerpt(post.getContent()));

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    /**
     * DELETE /api/posts/{slug} - 게시글 삭제 (기존 Next.js와 동일)
     */
    @Transactional
    @SuppressWarnings("null")
    public void delete(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        postRepository.delete(post);
    }

    private void validateCreateRequest(CreatePostRequest req) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()
                || req.getContent() == null || req.getContent().isBlank()
                || req.getCategoryId() == null) {
            throw new RuntimeException("Missing required fields");
        }
        if (req.getTitle().length() > TITLE_MAX) {
            throw new RuntimeException("제목이 너무 깁니다.");
        }
        if (req.getContent().length() > CONTENT_MAX) {
            throw new RuntimeException("내용이 너무 깁니다.");
        }
        validateImageCount(req.getContent());
        if (req.getCategoryId() < 1) {
            throw new RuntimeException("잘못된 카테고리입니다.");
        }
    }

    private void validateImageCount(String content) {
        long imageCount = MARKDOWN_IMAGE_PATTERN.matcher(content != null ? content : "").results().count();
        if (imageCount > IMAGE_MAX) {
            throw new RuntimeException("게시글에는 이미지를 최대 " + IMAGE_MAX + "장까지 넣을 수 있습니다.");
        }
    }

    private String normalizeThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) return null;
        if (thumbnailUrl.length() > 1000) throw new RuntimeException("썸네일 URL이 너무 깁니다.");
        return thumbnailUrl.trim();
    }

    private String resolveThumbnailUrl(String thumbnailUrl, String content) {
        String normalized = normalizeThumbnailUrl(thumbnailUrl);
        if (normalized != null) return normalized;
        var matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(content != null ? content : "");
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 기존 Next.js와 동일한 슬러그 생성 */
    private String generateSlugFromTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9가-힣]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /** 기존 Next.js와 동일한 요약 생성 */
    private String generateExcerpt(String content) {
        if (content == null || content.isEmpty()) return "";
        String processed = content.replaceAll("[#*`]", "").replace("\n", " ");
        return processed.substring(0, Math.min(150, processed.length())) + "...";
    }

    private PostResponse toResponse(Post p) {
        PostResponse r = new PostResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setContent(p.getContent());
        r.setExcerpt(p.getExcerpt());
        r.setCategoryId(p.getCategoryId());
        r.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : null);
        r.setSlug(p.getSlug());
        r.setGithubCommitUrl(p.getGithubCommitUrl());
        r.setThumbnailUrl(p.getThumbnailUrl());
        r.setIsPublic(p.getIsPublic());
        r.setAuthorId(p.getAuthorId());
        r.setLikesCount(p.getLikesCount() != null ? p.getLikesCount() : 0);
        r.setCommentsCount(p.getCommentsCount() != null ? p.getCommentsCount() : 0);
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    private PostSummaryResponse toPostSummaryResponse(PublicPostSummaryProjection p) {
        return new PostSummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getExcerpt(),
                p.getCategoryName(),
                p.getSlug(),
                p.getThumbnailUrl(),
                p.getLikesCount(),
                p.getCommentsCount(),
                p.getCreatedAt());
    }

    private AdminPostSummaryResponse toAdminPostSummaryResponse(AdminPostSummaryProjection p) {
        return new AdminPostSummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getCategoryName(),
                p.getSlug(),
                p.getIsPublic(),
                p.getLikesCount(),
                p.getCommentsCount(),
                p.getCreatedAt());
    }
}
