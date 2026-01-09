package com.notecurve.messageboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.messageboard.repository.MessageBoardRepository;
import com.notecurve.user.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageBoardService {

    private final MessageBoardRepository messageBoardRepository;

    // 게시판 생성
    public MessageBoard createMessageBoard(String title, User user) {
        MessageBoard messageBoard = new MessageBoard();
        messageBoard.setTitle(title);
        messageBoard.setUser(user);

        return messageBoardRepository.save(messageBoard);
    }

    // 전체 게시판 조회
    public List<MessageBoard> getAllMessageBoards() {
        return messageBoardRepository.findAll();
    }

    // 게시판 조회
    public MessageBoard getMessageBoard(Long id) {
        return messageBoardRepository.findById(id).orElse(null);
    }

    // 게시판 삭제
    public boolean deleteMessageBoard(Long id, User user) {
        MessageBoard messageBoard = messageBoardRepository.findById(id).orElse(null);
        if (messageBoard != null && messageBoard.getUser().getId().equals(user.getId())) {
            messageBoardRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
