package com.blog.blog_backend.model.dto.response;

public class StatsResponse {
    private long totalPosts;
    private long totalLikes;
    private long totalComments;
    private long totalVisitors;

    public long getTotalPosts() { return totalPosts; }
    public void setTotalPosts(long totalPosts) { this.totalPosts = totalPosts; }
    public long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }
    public long getTotalComments() { return totalComments; }
    public void setTotalComments(long totalComments) { this.totalComments = totalComments; }
    public long getTotalVisitors() { return totalVisitors; }
    public void setTotalVisitors(long totalVisitors) { this.totalVisitors = totalVisitors; }
}
