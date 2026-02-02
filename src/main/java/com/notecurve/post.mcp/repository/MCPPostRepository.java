package com.notecurve.post.mcp.repository;

import com.notecurve.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MCPPostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE (:category IS NULL OR LOWER(p.category) = LOWER(:category)) ORDER BY p.date DESC")
    List<Post> findPostsByCategory(
            @Param("category") String category,
            Pageable pageable
    );
}
