package com.blog.blog_backend.controller;

import com.blog.blog_backend.service.ImageService;
import com.blog.blog_backend.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 이미지 업로드(POST) + 업로드된 파일 서빙(GET)
 */
@RestController
public class ImageController {

    private static final String UPLOAD_PATH = "/api/uploads/image";
    private static final String FILES_PREFIX = "/api/files/";

    @Value("${storage.location}")
    private String storageLocation;

    @Autowired
    private ImageService imageService;
    @Autowired
    private AuthUtil authUtil;

    /** POST /api/uploads/image - 관리자만, multipart. content에 넣을 URL 반환 */
    @PostMapping(UPLOAD_PATH)
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (!authUtil.isAuthenticatedAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "권한이 없습니다."));
        }
        try {
            String relativePath = imageService.saveImage(file);
            String url = FILES_PREFIX + relativePath;
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "이미지 저장 실패"));
        }
    }

    /** GET /api/files/{path} - 업로드된 이미지 서빙 (인증 없음) */
    @GetMapping("/api/files/{*path}")
    public ResponseEntity<Resource> serveFile(@PathVariable("path") String path) {
        if (path == null || path.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        path = path.replace("\\", "/").replaceAll("^/", "");
        Path filePath = Paths.get(storageLocation).resolve(path).normalize();
        if (!filePath.startsWith(Paths.get(storageLocation).normalize())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Resource resource = new PathResource(filePath);
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = getContentType(path);
            MediaType mediaType = MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream");
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
