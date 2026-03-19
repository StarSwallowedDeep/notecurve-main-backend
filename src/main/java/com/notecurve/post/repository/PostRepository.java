package com.notecurve.post.repository;

import com.notecurve.post.domain.Post;
import com.notecurve.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Post와 User를 한 번에 fetch
    @Query("SELECT p FROM Post p JOIN FETCH p.user")
    List<Post> findAllWithUser();

    // 단건 조회 시 Post와 User를 한 번에 fetch
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
    Optional<Post> findByIdWithUser(@Param("postId") Long postId);

    List<Post> findByUser(User user);
}
