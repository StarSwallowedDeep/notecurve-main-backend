package com.notecurve.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.notecurve.category.domain.Category;
import com.notecurve.category.repository.CategoryRepository;
import com.notecurve.note.domain.Note;
import com.notecurve.note.dto.NoteDTO;
import com.notecurve.note.dto.NotesRequest;
import com.notecurve.note.repository.NoteRepository;
import com.notecurve.notefile.service.NoteFileService;
import com.notecurve.user.domain.User;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteFileService noteFileService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private NoteService noteService;

    // ========== getNotesByUser 테스트 ==========

    @Test
    @DisplayName("사용자별 노트 조회 성공 - 노트 리스트 반환")
    void getNotesByUser_success() {
        // Given
        User testUser = mock(User.class);
        Note testNote1 = mock(Note.class);
        Note testNote2 = mock(Note.class);
        when(noteRepository.findByUserWithCategory(testUser)).thenReturn(List.of(testNote1, testNote2));

        // When
        List<Note> result = noteService.getNotesByUser(testUser);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("사용자별 노트 조회 - 노트 없으면 빈 리스트 반환")
    void getNotesByUser_empty() {
        // Given
        User testUser = mock(User.class);
        when(noteRepository.findByUserWithCategory(testUser)).thenReturn(new ArrayList<>());

        // When
        List<Note> result = noteService.getNotesByUser(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getNoteWithRelations 테스트 ==========

    @Test
    @DisplayName("특정 노트 조회 성공 - 노트 반환")
    void getNoteWithRelations_success() {
        // Given
        User testUser = mock(User.class);
        Note testNote = mock(Note.class);
        when(noteRepository.findByIdAndUserWithCategory(1L, testUser)).thenReturn(Optional.of(testNote));

        // When
        Note result = noteService.getNoteWithRelations(1L, testUser);

        // Then
        assertThat(result).isEqualTo(testNote);
    }

    @Test
    @DisplayName("특정 노트 조회 실패 - 존재하지 않는 노트")
    void getNoteWithRelations_notFound() {
        // Given
        User testUser = mock(User.class);
        when(noteRepository.findByIdAndUserWithCategory(999L, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.getNoteWithRelations(999L, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Note not found or unauthorized");
    }

    // ========== createNotes 테스트 ==========

    @Test
    @DisplayName("노트 생성 성공 - NoteDTo 리스트 반환")
    void createNotes_success() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Category testCategory = mock(Category.class);
        when(testCategory.getId()).thenReturn(1L);
        when(testCategory.getUser()).thenReturn(testUser);

        NoteDTO noteDTO = NoteDTO.builder()
                .title("testTitle")
                .content("testContent")
                .build();

        NotesRequest notesRequest = new NotesRequest();
        notesRequest.setCategory(1L);
        notesRequest.setNotes(List.of(noteDTO));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        Note savedNote = mock(Note.class);
        when(savedNote.getTitle()).thenReturn("testTitle");
        when(savedNote.getContent()).thenReturn("testContent");
        when(savedNote.getCategory()).thenReturn(testCategory);
        when(noteRepository.save(any())).thenReturn(savedNote);

        // When
        List<NoteDTO> result = noteService.createNotes(testUser, notesRequest);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("testTitle");
    }

    @Test
    @DisplayName("노트 생성 실패 - 카테고리 없으면 예외 발생")
    void createNotes_fail_noCategory() {
        // Given
        User testUser = mock(User.class);
        NotesRequest notesRequest = new NotesRequest();
        notesRequest.setCategory(null);

        // When & Then
        assertThatThrownBy(() -> noteService.createNotes(testUser, notesRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("카테고리는 필수입니다.");
    }

    @Test
    @DisplayName("노트 생성 실패 - 카테고리 접근 권한 없으면 예외 발생")
    void createNotes_fail_unauthorizedCategory() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(2L);

        Category testCategory = mock(Category.class);
        when(testCategory.getUser()).thenReturn(otherUser);

        NotesRequest notesRequest = new NotesRequest();
        notesRequest.setCategory(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When & Then
        assertThatThrownBy(() -> noteService.createNotes(testUser, notesRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Forbidden category access");
    }

    // ========== deleteNote 테스트 ==========

    @Test
    @DisplayName("노트 삭제 성공 - 파일 없는 노트 삭제")
    void deleteNote_success_noFiles() throws Exception {
        // Given
        Note testNote = mock(Note.class);
        when(testNote.getFiles()).thenReturn(new ArrayList<>());

        // When
        noteService.deleteNote(testNote);

        // Then
        verify(noteFileService, never()).deletePhysicalFile(any());
        verify(noteRepository, times(1)).delete(testNote);
    }

    @Test
    @DisplayName("노트 삭제 성공 - 파일 있는 노트 삭제 시 파일도 함께 삭제")
    void deleteNote_success_withFiles() throws Exception {
        // Given
        com.notecurve.notefile.domain.NoteFile testFile1 = mock(com.notecurve.notefile.domain.NoteFile.class);
        com.notecurve.notefile.domain.NoteFile testFile2 = mock(com.notecurve.notefile.domain.NoteFile.class);

        Note testNote = mock(Note.class);
        when(testNote.getFiles()).thenReturn(List.of(testFile1, testFile2));

        // When
        noteService.deleteNote(testNote);

        // Then
        verify(noteFileService, times(1)).deletePhysicalFile(testFile1);
        verify(noteFileService, times(1)).deletePhysicalFile(testFile2);
        verify(noteRepository, times(1)).delete(testNote);
    }

    // ========== isUserAuthorizedForCategory 테스트 ==========

    @Test
    @DisplayName("카테고리 권한 체크 성공 - 본인 카테고리면 true 반환")
    void isUserAuthorizedForCategory_authorized() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        Category testCategory = mock(Category.class);
        when(testCategory.getUser()).thenReturn(testUser);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        boolean result = noteService.isUserAuthorizedForCategory(1L, testUser);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("카테고리 권한 체크 실패 - 타인 카테고리면 false 반환")
    void isUserAuthorizedForCategory_unauthorized() {
        // Given
        User testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        User otherUser = mock(User.class);
        when(otherUser.getId()).thenReturn(2L);

        Category testCategory = mock(Category.class);
        when(testCategory.getUser()).thenReturn(otherUser);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        boolean result = noteService.isUserAuthorizedForCategory(1L, testUser);

        // Then
        assertThat(result).isFalse();
    }

    // ========== convertToDTO 테스트 ==========

    @Test
    @DisplayName("노트 DTO 변환 성공 - 카테고리 있는 노트")
    void convertToDTO_withCategory() {
        // Given
        Category testCategory = mock(Category.class);
        when(testCategory.getId()).thenReturn(1L);
        when(testCategory.getName()).thenReturn("testCategory");

        Note testNote = mock(Note.class);
        when(testNote.getId()).thenReturn(1L);
        when(testNote.getTitle()).thenReturn("testTitle");
        when(testNote.getContent()).thenReturn("testContent");
        when(testNote.getCategory()).thenReturn(testCategory);

        // When
        NoteDTO result = noteService.convertToDTO(testNote);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("testTitle");
        assertThat(result.getCategory().getName()).isEqualTo("testCategory");
    }

    @Test
    @DisplayName("노트 DTO 변환 성공 - 카테고리 없는 노트")
    void convertToDTO_withoutCategory() {
        // Given
        Note testNote = mock(Note.class);
        when(testNote.getId()).thenReturn(1L);
        when(testNote.getTitle()).thenReturn("testTitle");
        when(testNote.getContent()).thenReturn("testContent");
        when(testNote.getCategory()).thenReturn(null);

        // When
        NoteDTO result = noteService.convertToDTO(testNote);

        // Then
        assertThat(result.getCategory().getName()).isEqualTo("No Category");
    }
}
