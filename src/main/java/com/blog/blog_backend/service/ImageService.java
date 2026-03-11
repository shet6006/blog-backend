package com.blog.blog_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    @Value("${storage.location}")
    private String storageLocation;

    /**
     * 이미지 저장 후 content에 넣을 상대 URL 경로 반환 (예: images/2025/02/uuid.jpg)
     * 컨트롤러에서 "/api/files/" + 반환값 으로 공개 URL 조합
     */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            throw new IllegalArgumentException("허용되지 않은 이미지 형식입니다. (jpeg, png, gif, webp만 가능)");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
        }

        String ext = ALLOWED_CONTENT_TYPES.get(contentType);
        String subDir = "images/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID() + "." + ext;

        Path dir = Paths.get(storageLocation, subDir);
        Path targetFile = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), targetFile);
            return subDir + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장에 실패했습니다.", e);
        }
    }
}
