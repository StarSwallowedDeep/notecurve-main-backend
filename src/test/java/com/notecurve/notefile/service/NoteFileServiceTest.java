package com.notecurve.notefile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.AfterEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.notecurve.note.domain.Note;
import com.notecurve.note.repository.NoteRepository;
import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.notefile.dto.NoteFileDTO;
import com.notecurve.notefile.repository.NoteFileRepository;
import com.notecurve.user.domain.User;

@ExtendWith(MockitoExtension.class)
class NoteFileServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private NoteFileRepository fileRepository;

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteFileService noteFileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noteFileService, "uploadDir", tempDir.toString());
        // 트랜잭션 동기화 활성화
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
    }

    // ========== uploadFile 테스트 ==========

    @Test
    @DisplayName("파일 업로드 성공 - NoteFile 반환")
    void uploadFile_success() throws IOException {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        MockMultipartFile testFile = new MockMultipartFile(
                "testFile",
                "testImage.jpg",
                "image/jpeg",
                "testContent".getBytes()
        );

        NoteFile savedNoteFile = mock(NoteFile.class);
        when(fileRepository.save(any())).thenReturn(savedNoteFile);

        // When
        NoteFile result = noteFileService.uploadFile(1L, testFile, 1L);

        // Then
        assertThat(result).isEqualTo(savedNoteFile);
        verify(fileRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 노트 없으면 예외 발생")
    void uploadFile_fail_noteNotFound() {
        // Given
        MockMultipartFile testFile = new MockMultipartFile(
                "testFile",
                "testImage.jpg",
                "image/jpeg",
                "testContent".getBytes()
        );
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteFileService.uploadFile(999L, testFile, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("노트를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("파일 업로드 실패 - 권한 없으면 예외 발생")
    void uploadFile_fail_unauthorized() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        MockMultipartFile testFile = new MockMultipartFile(
                "testFile",
                "testImage.jpg",
                "image/jpeg",
                "testContent".getBytes()
        );

        // When & Then - 다른 userId(2L)로 업로드 시도
        assertThatThrownBy(() -> noteFileService.uploadFile(1L, testFile, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 노트에 업로드할 권한이 없습니다.");
    }

    // ========== getFilesByNoteId 테스트 ==========

    @Test
    @DisplayName("노트별 파일 조회 성공 - NoteFileDTO 리스트 반환")
    void getFilesByNoteId_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        NoteFile testNoteFile = mock(NoteFile.class);
        when(testNoteFile.getId()).thenReturn(1L);
        when(testNoteFile.getOriginalName()).thenReturn("testImage.jpg");
        when(testNoteFile.getFileType()).thenReturn("image/jpeg");
        when(testNoteFile.getFileSize()).thenReturn(100L);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        when(fileRepository.findByNote(testNote)).thenReturn(List.of(testNoteFile));

        // When
        List<NoteFileDTO> result = noteFileService.getFilesByNoteId(1L, 1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalName()).isEqualTo("testImage.jpg");
    }

    @Test
    @DisplayName("노트별 파일 조회 실패 - 권한 없으면 예외 발생")
    void getFilesByNoteId_fail_unauthorized() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        // When & Then - 다른 userId(2L)로 조회 시도
        assertThatThrownBy(() -> noteFileService.getFilesByNoteId(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 노트에 접근할 권한이 없습니다.");
    }

    // ========== deleteFile 테스트 ==========

    @Test
    @DisplayName("파일 삭제 성공 - DB에서 파일 삭제")
    void deleteFile_success() throws IOException {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        NoteFile testNoteFile = new NoteFile();
        testNoteFile.setNote(testNote);
        testNoteFile.setFilePath("notes_1");
        testNoteFile.setStoredName("testImage.jpg");

        // 실제 파일 생성
        Path userFolder = tempDir.resolve("notes_1");
        Files.createDirectories(userFolder);
        Files.createFile(userFolder.resolve("testImage.jpg"));

        when(fileRepository.findById(1L)).thenReturn(Optional.of(testNoteFile));

        // When
        noteFileService.deleteFile(1L, 1L);

        // Then
        verify(fileRepository, times(1)).delete(testNoteFile);
    }

    @Test
    @DisplayName("파일 삭제 실패 - 파일 없으면 예외 발생")
    void deleteFile_fail_fileNotFound() {
        // Given
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteFileService.deleteFile(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("파일을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("파일 삭제 실패 - 권한 없으면 예외 발생")
    void deleteFile_fail_unauthorized() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Note testNote = mock(Note.class);
        when(testNote.getUser()).thenReturn(testUser);

        NoteFile testNoteFile = mock(NoteFile.class);
        when(testNoteFile.getNote()).thenReturn(testNote);

        when(fileRepository.findById(1L)).thenReturn(Optional.of(testNoteFile));

        // When & Then - 다른 userId(2L)로 삭제 시도
        assertThatThrownBy(() -> noteFileService.deleteFile(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 파일을 삭제할 권한이 없습니다.");
    }

    // ========== deletePhysicalFile 테스트 ==========

    @Test
    @DisplayName("실제 파일 삭제 성공 - 파일이 실제로 삭제됨")
    void deletePhysicalFile_success() throws IOException {
        // Given
        Path userFolder = tempDir.resolve("notes_1");
        Files.createDirectories(userFolder);
        Path testFilePath = userFolder.resolve("testImage.jpg");
        Files.createFile(testFilePath);

        NoteFile testNoteFile = new NoteFile();
        testNoteFile.setFilePath("notes_1");
        testNoteFile.setStoredName("testImage.jpg");

        assertThat(Files.exists(testFilePath)).isTrue();

        // When
        noteFileService.deletePhysicalFile(testNoteFile);

        // Then
        assertThat(Files.exists(testFilePath)).isFalse();
    }
}
