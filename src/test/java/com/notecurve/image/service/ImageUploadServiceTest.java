package com.notecurve.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

class ImageUploadServiceTest {

    // 테스트용 임시 디렉토리 (테스트 끝나면 자동 삭제)
    @TempDir
    Path tempDir;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() throws IOException {
        imageUploadService = new ImageUploadService(tempDir.toString());
    }

    // ========== validateFiles 테스트 ==========

    @Test
    @DisplayName("파일 검증 성공 - 허용된 확장자는 빈 리스트 반환")
    void validateFiles_allowedExtension_returnsEmpty() {
        // Given
        MultipartFile testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("testImage.jpg");

        // When
        List<String> result = imageUploadService.validateFiles(List.of(testFile));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("파일 검증 실패 - 허용되지 않는 확장자는 리스트에 추가")
    void validateFiles_notAllowedExtension_returnsInvalidList() {
        // Given
        MultipartFile testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("testFile.exe");

        // When
        List<String> result = imageUploadService.validateFiles(List.of(testFile));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(".exe");
    }

    @Test
    @DisplayName("파일 검증 실패 - 확장자 없는 파일은 리스트에 추가")
    void validateFiles_noExtension_returnsInvalidList() {
        // Given
        MultipartFile testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("testFile");

        // When
        List<String> result = imageUploadService.validateFiles(List.of(testFile));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains("파일 이름이 없거나 확장자가 없습니다.");
    }

    @Test
    @DisplayName("파일 검증 실패 - 파일 이름이 null이면 리스트에 추가")
    void validateFiles_nullFilename_returnsInvalidList() {
        // Given
        MultipartFile testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn(null);

        // When
        List<String> result = imageUploadService.validateFiles(List.of(testFile));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains("파일 이름이 없거나 확장자가 없습니다.");
    }

    @Test
    @DisplayName("파일 검증 - 여러 파일 중 일부만 허용되지 않는 경우")
    void validateFiles_mixedFiles_returnsOnlyInvalidList() {
        // Given
        MultipartFile validFile = mock(MultipartFile.class);
        when(validFile.getOriginalFilename()).thenReturn("testImage.png");

        MultipartFile invalidFile = mock(MultipartFile.class);
        when(invalidFile.getOriginalFilename()).thenReturn("testFile.exe");

        // When
        List<String> result = imageUploadService.validateFiles(List.of(validFile, invalidFile));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(".exe");
    }

    // ========== saveFile 테스트 ==========

    @Test
    @DisplayName("파일 저장 성공 - 파일명 반환 및 실제 파일 존재 확인")
    void saveFile_success() throws IOException {
        // Given
        MultipartFile testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("testImage.jpg");
        when(testFile.getInputStream()).thenReturn(new ByteArrayInputStream("testContent".getBytes()));

        // When
        String savedFilename = imageUploadService.saveFile(testFile);

        // Then
        assertThat(savedFilename).endsWith(".jpg");
        assertThat(Files.exists(tempDir.resolve(savedFilename))).isTrue();
    }

    // ========== deleteFile 테스트 ==========

    @Test
    @DisplayName("파일 삭제 성공 - 파일이 실제로 삭제됨")
    void deleteFile_success() throws IOException {
        // Given - 실제 파일 먼저 생성
        Path testFile = tempDir.resolve("testImage.jpg");
        Files.createFile(testFile);
        assertThat(Files.exists(testFile)).isTrue();

        // When
        imageUploadService.deleteFile("/images/testImage.jpg");

        // Then
        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("파일 삭제 - 존재하지 않는 파일 삭제 시 예외 없이 통과")
    void deleteFile_notExist_noException() {
        // Given
        String notExistUrl = "/images/notExistFile.jpg";

        // When & Then - 예외 없이 통과해야 함
        imageUploadService.deleteFile(notExistUrl);
    }
}
