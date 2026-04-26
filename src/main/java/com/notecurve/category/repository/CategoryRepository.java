package com.notecurve.category.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import com.notecurve.category.domain.Category;
import com.notecurve.user.domain.User;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 'notes' 속성을 eager 로딩하여 카테고리와 그에 연결된 노트까지 함께 조회
    @EntityGraph(attributePaths = "notes")
    List<Category> findByUser(User user);

    // 카테고리 ID와 사용자가 일치하는 경우, 존재하면 카테고리 반환
    Optional<Category> findByIdAndUser(Long id, User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Category c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
