package com.blog.blog_backend;

import com.blog.blog_backend.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PostResponseContractTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostService postService;

    @Test
    void listOmitsContentButDetailIncludesIt() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].excerpt").exists())
                .andExpect(jsonPath("$.posts[0].content").doesNotExist())
                .andExpect(jsonPath("$.posts[0].is_public").doesNotExist())
                .andExpect(jsonPath("$.posts[0].category_id").doesNotExist())
                .andExpect(jsonPath("$.posts[0].github_commit_url").doesNotExist())
                .andExpect(jsonPath("$.posts[0].author_id").doesNotExist())
                .andExpect(jsonPath("$.posts[0].updated_at").doesNotExist());

        mockMvc.perform(get("/api/posts/251126-til"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void adminListUsesAdminProjection() {
        var response = postService.findAdminPosts(1, 10);

        assertFalse(response.getPosts().isEmpty());
        assertNotNull(response.getPosts().get(0).getIsPublic());
    }
}
