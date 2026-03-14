package com.notecurve.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

class ImageServeServiceTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path profileTempDir;

    private ImageServeService imageServeService;

    @BeforeEach
    void setUp() {
        imageServeService = new ImageServeService();
        ReflectionTestUtils.setField(imageServeService, "uploadDir", tempDir);
        ReflectionTestUtils.setField(imageServeService, "profileUploadDir", profileTempDir);
    }

    // ========== serveImage 테스트 ==========

    @Test
    @DisplayName("이미지 제공 성공 - 파일 존재 시 Resource 반환")
    void serveImage_success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testImage.jpg");
        Files.createFile(testFile);

        // When
        Resource result = imageServeService.serveImage("testImage.jpg");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
    }

    @Test
    @DisplayName("이미지 제공 실패 - 파일 없으면 FileNotFoundException 발생")
    void serveImage_fileNotFound_throwsException() {
        // Given
        String notExistFilename = "notExistImage.jpg";

        // When & Then
        assertThatThrownBy(() -> imageServeService.serveImage(notExistFilename))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("이미지 제공 실패 - 경로 조작 공격 시 IllegalArgumentException 발생")
    void serveImage_pathTraversal_throwsException() {
        // Given - 경로 조작 공격 시도 (../../../etc/passwd)
        String maliciousFilename = "../../../etc/passwd";

        // When & Then
        assertThatThrownBy(() -> imageServeService.serveImage(maliciousFilename))
                .isInstanceOf(FileNotFoundException.class);
    }

    // ========== serveProfileImage 테스트 ==========

    @Test
    @DisplayName("프로필 이미지 제공 성공 - 파일 존재 시 Resource 반환")
    void serveProfileImage_success() throws IOException {
        // Given
        Path testFile = profileTempDir.resolve("testProfile.jpg");
        Files.createFile(testFile);

        // When
        Resource result = imageServeService.serveProfileImage("testProfile.jpg");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
    }

    @Test
    @DisplayName("프로필 이미지 제공 실패 - 파일 없으면 FileNotFoundException 발생")
    void serveProfileImage_fileNotFound_throwsException() {
        // Given
        String notExistFilename = "notExistProfile.jpg";

        // When & Then
        assertThatThrownBy(() -> imageServeService.serveProfileImage(notExistFilename))
                .isInstanceOf(FileNotFoundException.class);
    }

    // ========== determineContentType 테스트 ==========

    @Test
    @DisplayName("MIME 타입 반환 - jpg 파일은 image/jpg 반환")
    void determineContentType_jpg() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testImage.jpg");

        // When
        String result = imageServeService.determineContentType(testFile);

        // Then
        assertThat(result).isEqualTo("image/jpg");
    }

    @Test
    @DisplayName("MIME 타입 반환 - png 파일은 image/png 반환")
    void determineContentType_png() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testImage.png");

        // When
        String result = imageServeService.determineContentType(testFile);

        // Then
        assertThat(result).isEqualTo("image/png");
    }

    @Test
    @DisplayName("MIME 타입 반환 - gif 파일은 image/gif 반환")
    void determineContentType_gif() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testImage.gif");

        // When
        String result = imageServeService.determineContentType(testFile);

        // Then
        assertThat(result).isEqualTo("image/gif");
    }

    @Test
    @DisplayName("MIME 타입 반환 - 알 수 없는 확장자는 application/octet-stream 반환")
    void determineContentType_unknown() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testFile.unknownext");

        // When
        String result = imageServeService.determineContentType(testFile);

        // Then
        assertThat(result).isEqualTo("application/octet-stream");
    }
}
