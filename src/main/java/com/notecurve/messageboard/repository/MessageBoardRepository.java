package com.notecurve.messageboard.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.user.domain.User;

import java.util.List;

public interface MessageBoardRepository extends JpaRepository<MessageBoard, Long> {

    // @EntityGraph를 사용하여 MessageBoard와 관련된 Comments를 함께 로딩
    @EntityGraph(attributePaths = "comments")
    List<MessageBoard> findAll();

    void deleteByUser(User user);
}
