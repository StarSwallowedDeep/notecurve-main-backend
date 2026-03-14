package com.notecurve.category.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.notecurve.category.dto.CategoryDTO;
import com.notecurve.category.repository.CategoryRepository;
import com.notecurve.note.domain.Note;
import com.notecurve.note.service.NoteService;
import com.notecurve.user.domain.User;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private NoteService noteService;

    @InjectMocks
    private CategoryService categoryService;

    // ========== getCategoryByIdAndUser 테스트 ==========

    @Test
    @DisplayName("카테고리 단건 조회 성공 - 카테고리 반환")
    void getCategoryByIdAndUser_success() {
        // Given
        User testUser = mock(User.class);
        Category testCategory = mock(Category.class);
        // when(testCategory.getNotes()).thenReturn(new ArrayList<>()); ← 이 줄 제거
        when(categoryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testCategory));

        // When
        Optional<Category> result = categoryService.getCategoryByIdAndUser(1L, testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testCategory);
    }

    @Test
    @DisplayName("카테고리 단건 조회 실패 - 존재하지 않는 카테고리")
    void getCategoryByIdAndUser_notFound() {
        // Given
        User testUser = mock(User.class);
        when(categoryRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.getCategoryByIdAndUser(999L, testUser);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getCategoriesDTOByUser 테스트 ==========

    @Test
    @DisplayName("사용자별 카테고리 목록 조회 성공 - DTO 리스트 반환")
    void getCategoriesDTOByUser_success() {
        // Given
        User testUser = mock(User.class);

        Category testCategory1 = mock(Category.class);
        when(testCategory1.getId()).thenReturn(1L);
        when(testCategory1.getName()).thenReturn("카테고리1");
        when(testCategory1.getNotes()).thenReturn(new ArrayList<>());

        Category testCategory2 = mock(Category.class);
        when(testCategory2.getId()).thenReturn(2L);
        when(testCategory2.getName()).thenReturn("카테고리2");
        when(testCategory2.getNotes()).thenReturn(new ArrayList<>());

        when(categoryRepository.findByUser(testUser)).thenReturn(List.of(testCategory1, testCategory2));

        // When
        List<CategoryDTO> result = categoryService.getCategoriesDTOByUser(testUser);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("카테고리1");
        assertThat(result.get(1).getName()).isEqualTo("카테고리2");
    }

    @Test
    @DisplayName("사용자별 카테고리 목록 조회 - 카테고리 없으면 빈 리스트 반환")
    void getCategoriesDTOByUser_empty() {
        // Given
        User testUser = mock(User.class);
        when(categoryRepository.findByUser(testUser)).thenReturn(new ArrayList<>());

        // When
        List<CategoryDTO> result = categoryService.getCategoriesDTOByUser(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== getCategoryDTOByIdAndUser 테스트 ==========

    @Test
    @DisplayName("카테고리 DTO 단건 조회 성공 - DTO 반환")
    void getCategoryDTOByIdAndUser_success() {
        // Given
        User testUser = mock(User.class);
        Category testCategory = mock(Category.class);
        when(testCategory.getId()).thenReturn(1L);
        when(testCategory.getName()).thenReturn("testCategory");
        when(testCategory.getNotes()).thenReturn(new ArrayList<>());
        when(categoryRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testCategory));

        // When
        Optional<CategoryDTO> result = categoryService.getCategoryDTOByIdAndUser(1L, testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("testCategory");
    }

    @Test
    @DisplayName("카테고리 DTO 단건 조회 실패 - 존재하지 않는 카테고리")
    void getCategoryDTOByIdAndUser_notFound() {
        // Given
        User testUser = mock(User.class);
        when(categoryRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // When
        Optional<CategoryDTO> result = categoryService.getCategoryDTOByIdAndUser(999L, testUser);

        // Then
        assertThat(result).isEmpty();
    }

    // ========== saveCategory 테스트 ==========

    @Test
    @DisplayName("카테고리 저장 성공 - DTO 반환")
    void saveCategory_success() {
        // Given
        Category testCategory = mock(Category.class);
        when(testCategory.getId()).thenReturn(1L);
        when(testCategory.getName()).thenReturn("testCategory");
        when(testCategory.getNotes()).thenReturn(new ArrayList<>());
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);

        // When
        CategoryDTO result = categoryService.saveCategory(testCategory);

        // Then
        assertThat(result.getName()).isEqualTo("testCategory");
        verify(categoryRepository, times(1)).save(testCategory);
    }

    // ========== deleteCategoryAndNotes 테스트 ==========

    @Test
    @DisplayName("카테고리 삭제 성공 - 노트 먼저 삭제 후 카테고리 삭제")
    void deleteCategoryAndNotes_success() {
        // Given
        Note testNote1 = mock(Note.class);
        Note testNote2 = mock(Note.class);

        Category testCategory = mock(Category.class);
        when(testCategory.getNotes()).thenReturn(List.of(testNote1, testNote2));

        // When
        categoryService.deleteCategoryAndNotes(testCategory);

        // Then - 노트 2개 삭제 후 카테고리 삭제됐는지 검증
        verify(noteService, times(1)).deleteNote(testNote1);
        verify(noteService, times(1)).deleteNote(testNote2);
        verify(categoryRepository, times(1)).delete(testCategory);
    }

    @Test
    @DisplayName("카테고리 삭제 - 노트 없으면 카테고리만 삭제")
    void deleteCategoryAndNotes_noNotes() {
        // Given
        Category testCategory = mock(Category.class);
        when(testCategory.getNotes()).thenReturn(new ArrayList<>());

        // When
        categoryService.deleteCategoryAndNotes(testCategory);

        // Then
        verify(noteService, never()).deleteNote(any());
        verify(categoryRepository, times(1)).delete(testCategory);
    }
}
