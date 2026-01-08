package com.notecurve.messageboard.controller;

import java.util.List;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.notecurve.messageboard.dto.CommentRequest;
import com.notecurve.messageboard.dto.CommentDTO;
import com.notecurve.messageboard.service.CommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/message-board/{messageBoardId}")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long messageBoardId, 
            @Valid @RequestBody CommentRequest commentRequest) {

        CommentDTO commentDTO = commentService.createComment(messageBoardId, commentRequest.getContent());
        return ResponseEntity.ok(commentDTO);
    }

    @GetMapping("/message-board/{messageBoardId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByMessageBoard(@PathVariable Long messageBoardId) {
        List<CommentDTO> commentDTOs = commentService.getCommentsByMessageBoard(messageBoardId);
        return ResponseEntity.ok(commentDTOs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        boolean isDeleted = commentService.deleteComment(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
