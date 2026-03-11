package com.blog.blog_backend.controller;

import com.blog.blog_backend.model.entity.Comment;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.CommentRepository;
import com.blog.blog_backend.repository.PostRepository;
import com.blog.blog_backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GET /api/admin/comments - 기존 blog와 완전 동일 (관리자용 전체 댓글 목록)
 */
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentsController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<?> getComments(HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증되지 않은 요청입니다."));
        }

        List<Comment> comments = commentRepository.findAllByOrderByCreatedAtDesc();
        Set<Long> postIds = comments.stream().map(Comment::getPostId).collect(Collectors.toSet());
        Map<Long, Post> postMap = new HashMap<>();
        for (Long pid : postIds) {
            postRepository.findById(pid).ifPresent(p -> postMap.put(pid, p));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("post_id", c.getPostId());
            m.put("post_title", postMap.containsKey(c.getPostId()) ? postMap.get(c.getPostId()).getTitle() : null);
            m.put("post_slug", postMap.containsKey(c.getPostId()) ? postMap.get(c.getPostId()).getSlug() : null);
            m.put("author_name", c.getAuthorName());
            m.put("content", c.getContent());
            m.put("device_id", c.getDeviceId());
            m.put("is_admin", c.getIsAdmin());
            m.put("created_at", c.getCreatedAt());
            result.add(m);
        }

        return ResponseEntity.ok(Map.of("comments", result));
    }
}
