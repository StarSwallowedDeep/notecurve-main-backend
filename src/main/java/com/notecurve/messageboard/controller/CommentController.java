package com.notecurve.messageboard.controller;

import java.util.List;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.notecurve.messageboard.dto.CommentRequest;
import com.notecurve.messageboard.dto.CommentDTO;
import com.notecurve.messageboard.dto.AdminCommentDTO;
import com.notecurve.messageboard.service.CommentService;
import com.notecurve.auth.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/message-board/{messageBoardId}")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long messageBoardId,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CommentDTO commentDTO = commentService.createComment(
                messageBoardId,
                commentRequest.getContent(),
                userDetails.getUser().getId()
        );

        return ResponseEntity.ok(commentDTO);
    }

    @GetMapping("/message-board/{messageBoardId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByMessageBoard(
            @PathVariable Long messageBoardId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = (userDetails != null) ? userDetails.getUser().getId() : null;

        List<CommentDTO> commentDTOs = commentService.getCommentsByMessageBoard(messageBoardId, userId);

        return ResponseEntity.ok(commentDTOs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        commentService.deleteComment(id, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/all")
    public ResponseEntity<List<AdminCommentDTO>> getAllCommentsInternal() {
        return ResponseEntity.ok(commentService.getAllComments());
    }

    @DeleteMapping("/internal/{id}")
    public ResponseEntity<Void> forceDeleteComment(@PathVariable Long id) {
        commentService.adminDeleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
