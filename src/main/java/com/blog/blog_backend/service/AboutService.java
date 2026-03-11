package com.blog.blog_backend.service;

import com.blog.blog_backend.model.entity.AboutPage;
import com.blog.blog_backend.repository.AboutPageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AboutService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private AboutPageRepository aboutPageRepository;

    public Map<String, Object> getAbout() {
        try {
            Optional<AboutPage> opt = aboutPageRepository.findTopByOrderByIdDesc();
            if (opt.isEmpty()) {
                return defaultAbout();
            }
            AboutPage a = opt.get();
            List<String> techStack = parseTechStack(a.getTechStack());
            Map<String, Object> result = new HashMap<>();
            result.put("id", a.getId());
            result.put("title", a.getTitle());
            result.put("content", a.getContent());
            result.put("tech_stack", techStack);
            result.put("updated_at", a.getUpdatedAt() != null ? a.getUpdatedAt() : "");
            return result;
        } catch (Exception e) {
            return defaultAbout();
        }
    }

    public Map<String, Object> updateAbout(String title, String content, Object techStack) {
        List<String> techList = new ArrayList<>();
        if (techStack instanceof List) {
            for (Object o : (List<?>) techStack) {
                techList.add(o != null ? o.toString() : "");
            }
        }
        String techJson;
        try {
            techJson = OBJECT_MAPPER.writeValueAsString(techList);
        } catch (Exception e) {
            techJson = "[]";
        }

        Optional<AboutPage> opt = aboutPageRepository.findTopByOrderByIdDesc();
        AboutPage about;
        if (opt.isPresent()) {
            about = opt.get();
            about.setTitle(title);
            about.setContent(content);
            about.setTechStack(techJson);
        } else {
            about = new AboutPage();
            about.setTitle(title);
            about.setContent(content);
            about.setTechStack(techJson);
        }
        about = aboutPageRepository.save(about);

        List<String> parsed = parseTechStack(about.getTechStack());
        Map<String, Object> result = new HashMap<>();
        result.put("id", about.getId());
        result.put("title", about.getTitle());
        result.put("content", about.getContent());
        result.put("tech_stack", parsed);
        result.put("updated_at", about.getUpdatedAt() != null ? about.getUpdatedAt() : "");
        return result;
    }

    private Map<String, Object> defaultAbout() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", 0);
        m.put("title", "소개");
        m.put("content", "# 소개\n\n소개 페이지를 작성해주세요.");
        m.put("tech_stack", List.of());
        m.put("updated_at", new Date().toString());
        return m;
    }

    private List<String> parseTechStack(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed) || "undefined".equals(trimmed)) return List.of();
        try {
            if (trimmed.startsWith("[")) {
                return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            }
            return List.of(trimmed);
        } catch (Exception e) {
            return List.of();
        }
    }
}
