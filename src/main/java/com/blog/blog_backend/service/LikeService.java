package com.blog.blog_backend.service;

import com.blog.blog_backend.model.entity.Like;
import com.blog.blog_backend.model.entity.Post;
import com.blog.blog_backend.repository.LikeRepository;
import com.blog.blog_backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("null")
@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private PostRepository postRepository;

    /** GET /api/posts/{slug}/likes?deviceId= -> {liked, count} */
    public Map<String, Object> getLikeStatus(String slug, String deviceId) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        long postId = post.getId();

        boolean liked = likeRepository.findByPostIdAndDeviceId(postId, deviceId).isPresent();
        long count = likeRepository.countByPostId(postId);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    /** POST /api/posts/{slug}/likes body {deviceId} -> {liked, count} */
    public Map<String, Object> toggleLike(String slug, String deviceId) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        long postId = post.getId();

        Optional<Like> existing = likeRepository.findByPostIdAndDeviceId(postId, deviceId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            Like like = new Like();
            like.setPostId(postId);
            like.setDeviceId(deviceId);
            likeRepository.save(like);
            liked = true;
        }
        long count = likeRepository.countByPostId(postId);
        post.setLikesCount((int) count);
        postRepository.save(post);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    /** GET /api/likes?postId=&deviceId= */
    public Map<String, Object> getLikeStatusByPostId(long postId, String deviceId) {
        boolean liked = likeRepository.findByPostIdAndDeviceId(postId, deviceId).isPresent();
        long count = likeRepository.countByPostId(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    /** POST /api/likes body {post_id, device_id} */
    public Map<String, Object> toggleLikeByPostId(long postId, String deviceId) {
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("게시글을 찾을 수 없습니다.");
        }
        Optional<Like> existing = likeRepository.findByPostIdAndDeviceId(postId, deviceId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            Like like = new Like();
            like.setPostId(postId);
            like.setDeviceId(deviceId);
            likeRepository.save(like);
            liked = true;
        }
        Post post = postRepository.findById(postId).orElseThrow();
        long count = likeRepository.countByPostId(postId);
        post.setLikesCount((int) count);
        postRepository.save(post);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }
}
