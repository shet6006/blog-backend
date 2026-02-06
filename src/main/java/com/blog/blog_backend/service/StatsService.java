package com.blog.blog_backend.service;

import com.blog.blog_backend.model.dto.response.StatsResponse;
import com.blog.blog_backend.repository.CommentRepository;
import com.blog.blog_backend.repository.LikeRepository;
import com.blog.blog_backend.repository.PostRepository;
import com.blog.blog_backend.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private VisitorRepository visitorRepository;

    public StatsResponse getStats() {
        StatsResponse res = new StatsResponse();
        res.setTotalPosts(postRepository.countPublicPosts());
        res.setTotalLikes(likeRepository.count());
        res.setTotalComments(commentRepository.count());
        res.setTotalVisitors(visitorRepository.countDistinctByIpAddress());
        return res;
    }
}
