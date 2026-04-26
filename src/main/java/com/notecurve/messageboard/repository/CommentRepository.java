package com.notecurve.messageboard.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
