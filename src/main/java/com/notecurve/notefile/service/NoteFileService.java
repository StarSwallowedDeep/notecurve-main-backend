package com.notecurve.notefile.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.notecurve.notefile.dto.NoteFileDTO;
import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.note.domain.Note;
import com.notecurve.notefile.repository.NoteFileRepository;
import com.notecurve.note.repository.NoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteFileService {

    @Value("${note.upload-dir}")
    private String uploadDir;

    private final NoteFileRepository fileRepository;
    private final NoteRepository noteRepository;

    // 파일 업로드
    @Transactional
    public NoteFile uploadFile(Long noteId, MultipartFile file, Long userId) throws IOException {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("노트를 찾을 수 없습니다."));

        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("해당 노트에 업로드할 권한이 없습니다.");
        }

        String userFolder = "notes_" + userId;
        Path userFolderPath = Paths.get(uploadDir).resolve(userFolder);
        if (!Files.exists(userFolderPath)) {
            Files.createDirectories(userFolderPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String storedFileName = UUID.randomUUID() + fileExtension;

        Path targetLocation = userFolderPath.resolve(storedFileName);

        try {
            // 디스크에 파일 복사
            Files.copy(file.getInputStream(), targetLocation);

            // DB에 기록
            NoteFile newFile = new NoteFile();
            newFile.setOriginalName(originalFileName);
            newFile.setStoredName(storedFileName);
            newFile.setFilePath(userFolder);
            newFile.setFileType(file.getContentType());
            newFile.setFileSize(file.getSize());
            newFile.setNote(note);

            return fileRepository.save(newFile);
        } catch (Exception e) {
            // DB 저장 실패하면 방금 올린 파일 삭제
            Files.deleteIfExists(targetLocation);
            throw e;
        }
    }

    // 파일 조회 (노트 기준)
    @Transactional(readOnly = true)
    public List<NoteFileDTO> getFilesByNoteId(Long noteId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("노트를 찾을 수 없습니다."));

        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("해당 노트에 접근할 권한이 없습니다.");
        }

        List<NoteFile> files = fileRepository.findByNote(note);

        return files.stream()
                .map(file -> new NoteFileDTO(
                        file.getId(),
                        file.getOriginalName(),
                        file.getFileType(),
                        file.getFileSize()
                ))
                .toList();
    }

    // 파일 조회 (ID 기준)
    @Transactional(readOnly = true)
    public NoteFile getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
    }

    // 파일 다운로드
    public record DownloadFileResult(Resource resource, String originalName) {}

    public DownloadFileResult downloadFileWithName(Long fileId, Long userId) {
        NoteFile file = fileRepository.findWithNoteAndUserById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        if (!file.getNote().getUser().getId().equals(userId)) {
            throw new RuntimeException("해당 파일에 접근할 권한이 없습니다.");
        }

        Path path = Paths.get(uploadDir)
                .resolve(file.getFilePath())
                .resolve(file.getStoredName());

        if (!Files.exists(path)) {
            throw new RuntimeException("파일이 존재하지 않습니다.");
        }

        return new DownloadFileResult(new FileSystemResource(path.toFile()), file.getOriginalName());
    }

    // 파일 삭제 (DB + 실제 파일)
    @Transactional
    public void deleteFile(Long fileId, Long userId) throws IOException {
        NoteFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        if (!file.getNote().getUser().getId().equals(userId)) {
            throw new RuntimeException("해당 파일을 삭제할 권한이 없습니다.");
        }

        fileRepository.delete(file);

        // 트랜잭션 커밋 후 실제 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    deletePhysicalFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 이미지 서빙
    public Resource serveImage(Long userId, String requestedFileName) {
        Path userFolderPath = Paths.get(uploadDir)
                .resolve("notes_" + userId);

        NoteFile file = fileRepository.findByStoredNameAndNote_UserId(requestedFileName, userId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        Path filePath = userFolderPath.resolve(file.getStoredName());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("파일을 찾을 수 없습니다.");
        }

        return new FileSystemResource(filePath.toFile());
    }

    @Transactional
    public void uploadFiles(Long noteId, MultipartFile[] files, Long userId) throws IOException {
        for (MultipartFile file : files) {
            uploadFile(noteId, file, userId);
        }
    }

    @Transactional
    public void deleteFiles(List<Long> fileIds, Long userId) throws IOException {
        for (Long fileId : fileIds) {
            deleteFile(fileId, userId);
        }
    }

    // 이미지 파일 삭제
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void deleteFileByStoredName(String storedName) {
        fileRepository.findByStoredName(storedName).ifPresent(file -> {
            try {
                fileRepository.delete(file);
                deletePhysicalFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // 실제 파일 삭제
    public void deletePhysicalFile(NoteFile file) throws IOException {
        Path filePath = Paths.get(uploadDir)
                .resolve(file.getFilePath())
                .resolve(file.getStoredName());

        Files.deleteIfExists(filePath);
    }

    // UUID 파일명으로 URL 생성
    @Transactional
    public List<String> uploadFilesWithUrls(Long noteId, MultipartFile[] files, Long userId) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            NoteFile saved = uploadFile(noteId, file, userId);
            urls.add("/api/files/images/" + saved.getStoredName());
        }
        return urls;
    }
}
