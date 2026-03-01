package com.notecurve.notefile.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;  
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.notecurve.auth.security.UserDetailsImpl;
import com.notecurve.notefile.dto.NoteFileDTO;
import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.notefile.service.NoteFileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class NoteFileController {

    private final NoteFileService fileService;

    @Value("${note.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload/{noteId}")
    public ResponseEntity<List<String>> uploadFiles(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long noteId,
            @RequestParam MultipartFile[] files
    ) {
        Long userId = userDetails.getUser().getId();

        try {
            List<String> urls = fileService.uploadFilesWithUrls(noteId, files, userId);
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/note/{noteId}")
    public ResponseEntity<List<NoteFileDTO>> getFilesByNote(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long noteId
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(fileService.getFilesByNoteId(noteId, userId));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long fileId
    ) {
        Long userId = userDetails.getUser().getId();

        NoteFileService.DownloadFileResult result = fileService.downloadFileWithName(fileId, userId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(result.resource());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFiles(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody List<Long> fileIds
    ) {
        Long userId = userDetails.getUser().getId();

        try {
            fileService.deleteFiles(fileIds, userId);

            return ResponseEntity.ok("여러 파일 삭제 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> serveImage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String fileName
    ) {
        Long userId = userDetails.getUser().getId();

        try {
            Resource imageResource = fileService.serveImage(userId, fileName);
            return ResponseEntity.ok(imageResource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
