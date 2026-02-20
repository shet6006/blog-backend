package com.blog.blog_backend.service;

import com.blog.blog_backend.model.dto.request.CreateCommentRequest;
import com.blog.blog_backend.model.dto.response.CommentResponse;
import com.blog.blog_backend.model.entity.Comment;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.CommentRepository;
import com.blog.blog_backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9가-힣-]+$");
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int AUTHOR_NAME_MAX = 100;
    private static final int CONTENT_MAX = 5000;
    private static final int DEVICE_ID_MAX = 100;

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostRepository postRepository;

    /**
     * GET /api/comments/{slug}
     * slug에 해당하는 글이 없으면 빈 리스트 반환(200), 있으면 해당 글 댓글 목록 created_at ASC.
     */
    public List<CommentResponse> getCommentsBySlug(String slug) {
        if (!isValidSlug(slug)) {
            throw new RuntimeException("잘못된 요청입니다.");
        }
        Optional<Post> postOpt = postRepository.findBySlug(slug);
        if (postOpt.isEmpty()) {
            return List.of();
        }
        Long postId = postOpt.get().getId();
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * POST /api/comments/{slug}
     * 검증 후 댓글 저장, 해당 글 comments_count 갱신.
     */
    public void createComment(String slug, CreateCommentRequest request) {
        if (!isValidSlug(slug)) {
            throw new RuntimeException("잘못된 요청입니다.");
        }
        validateCreateRequest(request);

        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        String authorName = sanitizeAuthorName(request.getAuthorName());
        String content = request.getContent().trim();

        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setAuthorName(authorName);
        comment.setContent(content);
        comment.setDeviceId(request.getDeviceId());
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        updatePostCommentsCount(post);
    }

    /**
     * DELETE /api/comments/{slug}
     * body에 commentId 있으면 해당 댓글만(post_id 일치 시), 없으면 해당 글 댓글 전부 삭제 후 comments_count 갱신.
     */
    public void deleteComment(String slug, Long commentId) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        Long postId = post.getId();

        if (commentId != null) {
            commentRepository.deleteByIdAndPostId(commentId, postId);
        } else {
            commentRepository.deleteByPostId(postId);
        }
        updatePostCommentsCount(post);
    }

    private boolean isValidSlug(String slug) {
        return slug != null && SLUG_PATTERN.matcher(slug).matches() && slug.length() <= 255;
    }

    private void validateCreateRequest(CreateCommentRequest request) {
        if (request == null ||
                isBlank(request.getAuthorName()) ||
                isBlank(request.getContent()) ||
                isBlank(request.getDeviceId())) {
            throw new RuntimeException("필수 항목이 누락되었습니다.");
        }
        String name = request.getAuthorName().trim();
        if (name.length() > AUTHOR_NAME_MAX) {
            throw new RuntimeException("이름이 너무 깁니다. (최대 100자)");
        }
        String content = request.getContent().trim();
        if (content.length() > CONTENT_MAX) {
            throw new RuntimeException("내용이 너무 깁니다. (최대 5000자)");
        }
        String deviceId = request.getDeviceId();
        if (!DEVICE_ID_PATTERN.matcher(deviceId).matches() || deviceId.length() > DEVICE_ID_MAX) {
            throw new RuntimeException("잘못된 deviceId입니다.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String sanitizeAuthorName(String name) {
        if (name == null) return "";
        return name.replaceAll("<[^>]*>", "").trim();
    }

    private void updatePostCommentsCount(Post post) {
        long count = commentRepository.countByPostId(post.getId());
        post.setCommentsCount((int) count);
        postRepository.save(post);
    }

    private CommentResponse toResponse(Comment c) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId());
        r.setPostId(c.getPostId());
        r.setAuthorName(c.getAuthorName());
        r.setContent(c.getContent());
        r.setDeviceId(c.getDeviceId());
        r.setCreatedAt(c.getCreatedAt());
        r.setIsAdmin(c.getIsAdmin());
        return r;
    }
}
