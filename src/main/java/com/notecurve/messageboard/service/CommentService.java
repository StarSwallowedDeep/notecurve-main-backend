package com.notecurve.messageboard.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.notecurve.messageboard.dto.CommentDTO;
import com.notecurve.messageboard.dto.AdminCommentDTO;
import com.notecurve.messageboard.domain.Comment;
import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.messageboard.repository.CommentRepository;
import com.notecurve.messageboard.repository.MessageBoardRepository;
import com.notecurve.user.domain.User;
import com.notecurve.user.repository.UserRepository;
import com.notecurve.kafka.producer.EventProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MessageBoardRepository messageBoardRepository;
    private final UserRepository userRepository;
    private final EventProducer eventProducer;

    public CommentDTO createComment(Long messageBoardId, String content, Long userId) {
        MessageBoard messageBoard = getMessageBoard(messageBoardId);
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (commentRepository.existsByMessageBoardAndUser(messageBoard, currentUser)) {
            throw new RuntimeException("각 사용자는 하나의 댓글만 작성할 수 있습니다.");
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setMessageBoard(messageBoard);
        comment.setUser(currentUser);

        comment = commentRepository.save(comment);
        eventProducer.sendCommentEvent(
            "CREATED", 
            comment.getId(),
            comment.getUser().getId(),
            comment.getContent(), 
            currentUser.getName(),
            messageBoard.getId(),
            messageBoard.getTitle()
        );

        return convertToDTO(comment, currentUser.getId());
    }

    public List<CommentDTO> getCommentsByMessageBoard(Long messageBoardId, Long userId) {
        MessageBoard messageBoard = getMessageBoard(messageBoardId);
        List<Comment> comments = commentRepository.findByMessageBoard(messageBoard);

        return comments.stream()
                .map(comment -> convertToDTO(comment, userId))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글 작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
        eventProducer.sendCommentEvent("DELETED", id, null, null, null, null, null);
    }

    private MessageBoard getMessageBoard(Long messageBoardId) {
        return messageBoardRepository.findById(messageBoardId)
                .orElseThrow(() -> new RuntimeException("Message Board not found"));
    }

    private CommentDTO convertToDTO(Comment comment, Long userId) {
        boolean canDelete = comment.getUser().getId().equals(userId);
        return new CommentDTO(comment.getId(), comment.getContent(), canDelete);
    }

    public List<AdminCommentDTO> getAllComments() {
        return commentRepository.findAll().stream()
                .map(comment -> new AdminCommentDTO(
                    comment.getId(),
                    comment.getUser().getId(),
                    comment.getContent(),
                    comment.getUser().getName(),
                    comment.getMessageBoard().getId(), 
                    comment.getMessageBoard().getTitle()
                ))
                .collect(Collectors.toList());
    }

    public long countComments() {
        return commentRepository.count();
    }

    public void adminDeleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        commentRepository.delete(comment);
        
        eventProducer.sendCommentEvent("DELETED", id, null, null, null, null, null);
    }
}
