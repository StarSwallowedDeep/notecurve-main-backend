package com.notecurve.messageboard.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<MessageBoardDTO> createMessageBoard(@RequestBody MessageBoardDTO messageBoardDto) {
        MessageBoard messageBoard = messageBoardService.createMessageBoard(messageBoardDto.getTitle());
        MessageBoardDTO responseDto = convertToDTO(messageBoard, false);
        
        //return ResponseEntity.ok(messageBoard);
        return ResponseEntity.status(201).body(responseDto);
    }

    // 전체 게시판 조회
    @GetMapping
    public ResponseEntity<List<MessageBoardDTO>> getAllMessageBoards() {
        List<MessageBoard> messageBoards = messageBoardService.getAllMessageBoards();
        List<MessageBoardDTO> messageBoardDTOs = messageBoards.stream()
            // 댓글은 제외하고 변환
            .map(messageBoard -> convertToDTO(messageBoard, false))
            .collect(Collectors.toList());
        return ResponseEntity.ok(messageBoardDTOs);
    }

    // 게시판 조회
    @GetMapping("/{id}")
    public ResponseEntity<MessageBoardDTO> getMessageBoard(@PathVariable Long id) {
        MessageBoard messageBoard = messageBoardService.getMessageBoard(id);
        if (messageBoard != null) {
            // 게시판 조회 시, 댓글 포함 여부를 true로 설정
            MessageBoardDTO messageBoardDTO = convertToDTO(messageBoard, true); 
            return ResponseEntity.ok(messageBoardDTO);
        }
        return ResponseEntity.notFound().build();
    }

    // 게시판 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageBoard(@PathVariable Long id) {
        boolean isDeleted = messageBoardService.deleteMessageBoard(id);
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private MessageBoardDTO convertToDTO(MessageBoard messageBoard, boolean includeComments) {
        return new MessageBoardDTO(
            messageBoard.getId(),
            messageBoard.getTitle(),
            messageBoard.getFormattedCreatedAt(),
            includeComments ? commentService.getCommentsByMessageBoard(messageBoard.getId()) : null,
            messageBoard.getUser().getName()
        );
    }
}
