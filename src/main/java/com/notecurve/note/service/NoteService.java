package com.notecurve.note.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.notecurve.category.domain.Category;
import com.notecurve.category.repository.CategoryRepository;
import com.notecurve.note.domain.Note;
import com.notecurve.note.dto.NoteDTO;
import com.notecurve.note.dto.NotesRequest;
import com.notecurve.note.repository.NoteRepository;
import com.notecurve.notefile.service.NoteFileService;
import com.notecurve.user.domain.User;
import com.notecurve.category.dto.CategoryDTO;
import com.notecurve.notefile.domain.NoteFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteFileService noteFileService;
    private final CategoryRepository categoryRepository;

    // 사용자별 노트 조회
    public List<Note> getNotesByUser(User user) {
        return noteRepository.findByUserWithCategory(user);
    }

    // 카테고리별 노트 조회
    public List<Note> getNotesByCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to category");
        }

        return noteRepository.findByCategoryWithFetch(category);
    }

    // 특정 노트 조회
    public Note getNoteWithRelations(Long noteId, User user) {
        return noteRepository.findByIdAndUserWithCategory(noteId, user)
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));
    }

    // 노트 생성
    @Transactional
    public List<NoteDTO> createNotes(User user, NotesRequest notesRequest) {

        if (notesRequest.getCategory() == null) {
            throw new RuntimeException("카테고리는 필수입니다.");
        }

        if (!isUserAuthorizedForCategory(notesRequest.getCategory(), user)) {
            throw new RuntimeException("Forbidden category access");
        }

        final Category category = categoryRepository.findById(notesRequest.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return notesRequest.getNotes().stream().map(dto -> {
            Note note = Note.builder()
                    .title(dto.getTitle() != null ? dto.getTitle() : "제목 없음")
                    .content(dto.getContent() != null ? dto.getContent() : "내용 없음")
                    .user(user)
                    .category(category)
                    .build();

            Note saved = saveNote(note);
            return convertToDTO(saved);
        }).toList();
    }

    // 노트 수정
    @Transactional
    public NoteDTO updateNote(User user, Long noteId, NoteDTO noteDTO) {
        Note note = getNoteWithRelations(noteId, user);

        // 삭제된 이미지 URL 추출
        Set<String> oldImageUrls = extractImageUrls(note.getContent());
        Set<String> newImageUrls = extractImageUrls(noteDTO.getContent());
        Set<String> deletedImageUrls = new HashSet<>(oldImageUrls);
        deletedImageUrls.removeAll(newImageUrls);

        if (noteDTO.getDeletedFileIds() != null) {
            for (Long fileId : noteDTO.getDeletedFileIds()) {
                try {
                    noteFileService.deleteFile(fileId, user.getId());
                } catch (IOException e) {
                    throw new RuntimeException("파일 삭제 실패", e);
                }
            }
        }

        note.setTitle(noteDTO.getTitle() != null ? noteDTO.getTitle() : "제목 없음");
        note.setContent(noteDTO.getContent() != null ? noteDTO.getContent() : "내용 없음");

        Note updated = saveNote(note);

        // 커밋 후 삭제된 이미지 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deletedImageUrls.forEach(url -> {
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    noteFileService.deleteFileByStoredName(fileName);
                });
            }
        });

        return convertToDTO(updated);
    }

    // 노트 저장
    @Transactional
    public Note saveNote(Note note) {
        return noteRepository.save(note);
    }

    // 노트 삭제
    @Transactional
    public void deleteNote(Note note) {
        if (note.getFiles() != null) {
            for (NoteFile file : note.getFiles()) {
                try {
                    noteFileService.deletePhysicalFile(file);
                } catch (IOException e) {
                    throw new RuntimeException("파일 삭제 실패", e);
                }
            }
        }
        noteRepository.delete(note);
    }

    // 카테고리 권한 체크
    public boolean isUserAuthorizedForCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return category.getUser().getId().equals(user.getId());
    }

    // DTO 변환
    public NoteDTO convertToDTO(Note note) {
        Category category = note.getCategory();
        Long categoryId = category != null ? category.getId() : null;
        String categoryName = category != null ? category.getName() : "No Category";

        return NoteDTO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdDate(note.getCreatedDate())
                .category(new CategoryDTO(categoryId, categoryName))
                .build();
    }

    // 이미지 URL 추출 메서드
    private Set<String> extractImageUrls(String content) {
        if (content == null) return new HashSet<>();
        Set<String> urls = new HashSet<>();
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile("<img[^>]+src=\"([^\"]+)\"").matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }
}
