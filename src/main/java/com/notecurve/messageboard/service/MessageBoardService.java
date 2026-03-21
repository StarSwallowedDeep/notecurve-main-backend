package com.notecurve.messageboard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.messageboard.domain.Comment;
import com.notecurve.messageboard.repository.MessageBoardRepository;
import com.notecurve.messageboard.repository.CommentRepository;
import com.notecurve.kafka.producer.EventProducer;
import com.notecurve.user.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageBoardService {

    private final MessageBoardRepository messageBoardRepository;
    private final CommentRepository commentRepository;
    private final EventProducer eventProducer;

    // 게시판 생성
    public MessageBoard createMessageBoard(String title, User user) {
        MessageBoard messageBoard = new MessageBoard();
        messageBoard.setTitle(title);
        messageBoard.setUser(user);

        MessageBoard savedBoard = messageBoardRepository.save(messageBoard);
        eventProducer.sendMessageBoardEvent("CREATED", savedBoard.getId(), user.getId(), savedBoard.getTitle(), user.getName(), savedBoard.getFormattedCreatedAt());

        return savedBoard;
    }

    // 전체 게시판 조회
    public List<MessageBoard> getAllMessageBoards() {
        return messageBoardRepository.findAll();
    }

    // 게시판 조회
    public MessageBoard getMessageBoard(Long id) {
        return messageBoardRepository.findById(id).orElse(null);
    }

    // 게시판 삭제 (사용자용)
    @Transactional
    public boolean deleteMessageBoard(Long id, User user) {
        MessageBoard messageBoard = messageBoardRepository.findById(id).orElse(null);
        if (messageBoard != null && messageBoard.getUser().getId().equals(user.getId())) {
            
            // 실제 DB 삭제
            commentRepository.deleteByMessageBoard(messageBoard);
            messageBoardRepository.delete(messageBoard);

            // 게시판 삭제 이벤트 발행
            eventProducer.sendMessageBoardEvent("DELETED", id, null, null, null, null);
            
            return true;
        }
        return false;
    }

    // 게시판 삭제 (관리자용)
    @Transactional
    public void adminDeleteMessageBoard(Long id) {
        MessageBoard board = messageBoardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다."));

        commentRepository.deleteByMessageBoard(board);
        messageBoardRepository.delete(board);

        // 관리자 서버에 게시판 삭제 알림 발행
        eventProducer.sendMessageBoardEvent("DELETED", id, null, null, null, null);
    }
}
