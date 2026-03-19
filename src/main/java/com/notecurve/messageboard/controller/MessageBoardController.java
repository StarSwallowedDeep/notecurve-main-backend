package com.notecurve.messageboard.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.notecurve.auth.security.UserDetailsImpl;
import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.messageboard.dto.MessageBoardDTO;
import com.notecurve.messageboard.service.CommentService;
import com.notecurve.messageboard.service.MessageBoardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/message-boards")
@RequiredArgsConstructor
public class MessageBoardController {

    private final MessageBoardService messageBoardService;
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<MessageBoardDTO> createMessageBoard(
            @RequestBody MessageBoardDTO messageBoardDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        MessageBoard messageBoard = messageBoardService.createMessageBoard(
                messageBoardDto.getTitle(),
                userDetails.getUser()
        );
        
        MessageBoardDTO responseDto = convertToDTO(
            messageBoard,
            false,
            userDetails.getUser().getId()
        );

        //return ResponseEntity.ok(messageBoard);
        return ResponseEntity.status(201).body(responseDto);
    }

    // 전체 게시판 조회
    @GetMapping
    public ResponseEntity<List<MessageBoardDTO>> getAllMessageBoards() {
        List<MessageBoard> messageBoards = messageBoardService.getAllMessageBoards();
        List<MessageBoardDTO> messageBoardDTOs = messageBoards.stream()
            .map(messageBoard -> convertToDTO(messageBoard, false, null))
            .collect(Collectors.toList());
        return ResponseEntity.ok(messageBoardDTOs);
    }

    // 게시판 조회
    @GetMapping("/{id}")
    public ResponseEntity<MessageBoardDTO> getMessageBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        MessageBoard messageBoard = messageBoardService.getMessageBoard(id);

        if (messageBoard != null) {
            Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;

            // 댓글 포함 여부 true, 로그인 시 userId 전달
            MessageBoardDTO messageBoardDTO = convertToDTO(messageBoard, true, currentUserId);
            return ResponseEntity.ok(messageBoardDTO);
        }
        return ResponseEntity.notFound().build();
    }

    // 게시판 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        boolean isDeleted = messageBoardService.deleteMessageBoard(
                id,
                userDetails.getUser()
        );

        if (isDeleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/internal/all")
    public ResponseEntity<List<MessageBoardDTO>> getAllBoardsInternal() {
        // 엔티티가 아닌 DTO 리스트로 변환해서 반환
        List<MessageBoardDTO> dtos = messageBoardService.getAllMessageBoards().stream()
                .map(board -> convertToDTO(board, false, null)) // 댓글은 제외하고 변환
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/internal/{id}")
    public ResponseEntity<Void> forceDeleteBoard(@PathVariable Long id) {
        messageBoardService.adminDeleteMessageBoard(id);
        return ResponseEntity.noContent().build();
    }

    private MessageBoardDTO convertToDTO(MessageBoard messageBoard, boolean includeComments, Long userId) {
        return new MessageBoardDTO(
            messageBoard.getId(),
            messageBoard.getUser().getId(),
            messageBoard.getTitle(),
            messageBoard.getFormattedCreatedAt(),
            includeComments ? commentService.getCommentsByMessageBoard(messageBoard.getId(), userId) : null,
            messageBoard.getUser().getName()
        );
    }
}
