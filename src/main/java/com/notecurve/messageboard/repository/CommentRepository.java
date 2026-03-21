package com.notecurve.messageboard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.notecurve.messageboard.domain.Comment;
import com.notecurve.messageboard.domain.MessageBoard;
import com.notecurve.user.domain.User;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // @EntityGraph를 사용하여 Comment와 User를 함께 로딩
    @EntityGraph(attributePaths = "user")
    List<Comment> findByMessageBoard(MessageBoard messageBoard);
    
    boolean existsByMessageBoardAndUser(MessageBoard messageBoard, User user);

    void deleteByUser(User user);

    void deleteByMessageBoard(MessageBoard messageBoard);
}
