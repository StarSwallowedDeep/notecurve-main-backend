package com.notecurve.image.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {

    private final Path uploadDir;
    private final List<String> allowedExtensions = List.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".tiff", ".tif", ".ico"
    );

    public ImageUploadService(@Value("${file.upload-dir}") String uploadDirStr) throws IOException {
        this.uploadDir = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        // 업로드 디렉토리가 없다면 생성
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    // 파일 확장자 검사
    public List<String> validateFiles(List<MultipartFile> files) {
        List<String> invalidFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.contains(".")) {
                invalidFiles.add("파일 이름이 없거나 확장자가 없습니다.");
            } else {
                String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
                if (!allowedExtensions.contains(ext)) {
                    invalidFiles.add(ext);
                }
            }
        }
        return invalidFiles;
    }

    // 파일 저장 메서드
    public String saveFile(MultipartFile file) throws IOException {
        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
        String filename = UUID.randomUUID().toString() + ext;
        Path filePath = uploadDir.resolve(filename);

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
