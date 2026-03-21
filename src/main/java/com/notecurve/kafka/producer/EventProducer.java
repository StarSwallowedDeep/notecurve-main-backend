package com.notecurve.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.notecurve.kafka.event.UserEvent;
import com.notecurve.kafka.event.PostEvent;
import com.notecurve.kafka.event.CommentEvent;
import com.notecurve.kafka.event.MessageBoardEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_TOPIC = "user-events";
    private static final String POST_TOPIC = "post-events";
    private static final String COMMENT_TOPIC = "comment-events";
    private static final String MESSAGE_BOARD_TOPIC = "message-board-events";

    public void sendUserEvent(String type, Long userId, String loginId, String name, String role) {
        try {
            UserEvent event = new UserEvent(type, userId, loginId, name, role);
            kafkaTemplate.send(USER_TOPIC, event);
            log.info("UserEvent 발행: type={}, userId={}, name={}", type, userId, name);
        } catch (Exception e) {
            log.warn("UserEvent 발행 실패: {}", e.getMessage());
        }
    }

    public void sendPostEvent(String type, Long postId, Long userId, String title, String userName, LocalDate date) {
        try {
            PostEvent event = new PostEvent(type, postId, userId, title, userName, date);
            kafkaTemplate.send(POST_TOPIC, event);
            log.info("PostEvent 발행: type={}, title={}", type, title);
        } catch (Exception e) {
            log.warn("PostEvent 발행 실패: {}", e.getMessage());
        }
    }

    public void sendCommentEvent(String type, Long commentId, Long userId, String content, String userName, Long messageBoardId, String messageBoardTitle) {
        try {
            CommentEvent event = new CommentEvent(type, commentId, userId, content, userName, messageBoardId, messageBoardTitle);
            kafkaTemplate.send(COMMENT_TOPIC, event);
            log.info("CommentEvent 발행: type={}, commentId={}", type, commentId);
        } catch (Exception e) {
            log.warn("CommentEvent 발행 실패: {}", e.getMessage());
        }
    }

    public void sendMessageBoardEvent(String type, Long boardId, Long userId, String title, String userName, String createdAt) {
        try {
            MessageBoardEvent event = new MessageBoardEvent(type, boardId, userId, title, userName, createdAt);
            kafkaTemplate.send(MESSAGE_BOARD_TOPIC, event);
            log.info("MessageBoardEvent 발행: type={}, boardId={}, title={}", type, boardId, title);
        } catch (Exception e) {
            log.warn("MessageBoardEvent 발행 실패: {}", e.getMessage());
        }
    }
}
