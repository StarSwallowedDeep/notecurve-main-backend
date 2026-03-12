package com.notecurve.image.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ImageServeService {

    private static final Logger LOGGER = Logger.getLogger(ImageServeService.class.getName());
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "tiff", "tif", "ico"
    );

    @Value("${file.upload-dir}")
    private Path uploadDir;

    @Value("${profile.upload-dir}")
    private Path profileUploadDir;

    public Resource serveImage(String filename) throws IOException {
        String sanitizedFilename = FilenameUtils.getName(filename);
        Path filePath = uploadDir.resolve(sanitizedFilename).normalize();

        // 파일 경로 검증
        if (!filePath.startsWith(uploadDir)) {
            LOGGER.warning("Invalid file path: " + filePath);
            throw new IllegalArgumentException("Invalid file path");
        }

        Resource resource = new FileSystemResource(filePath);

        // 파일 존재 여부 확인
        if (!resource.exists() || !resource.isReadable()) {
            LOGGER.warning("File not found or unreadable: " + filePath);
            throw new FileNotFoundException("File not found: " + filePath);
        }

        return resource;
    }

    public Resource serveProfileImage(String filename) throws IOException {
        String sanitizedFilename = FilenameUtils.getName(filename);
        Path filePath = profileUploadDir.resolve(sanitizedFilename).normalize();

        if (!filePath.startsWith(profileUploadDir)) {
            LOGGER.warning("Invalid file path: " + filePath);
            throw new IllegalArgumentException("Invalid file path");
        }

        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            LOGGER.warning("File not found or unreadable: " + filePath);
            throw new FileNotFoundException("File not found: " + filePath);
        }

        return resource;
    }

    public String determineContentType(Path filePath) throws IOException {
        String ext = getExtension(filePath.getFileName().toString());

        if (ALLOWED_EXTENSIONS.contains(ext)) {
            return "image/" + ext;
        }

        return Files.probeContentType(filePath) != null ? Files.probeContentType(filePath) : "application/octet-stream";
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
