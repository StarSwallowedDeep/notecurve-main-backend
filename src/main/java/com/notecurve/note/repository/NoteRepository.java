package com.notecurve.note.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.notecurve.note.domain.Note;
import com.notecurve.user.domain.User;
import com.notecurve.category.domain.Category;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // 사용자별 노트 조회 (Category fetch만)
    @Query("""
           SELECT DISTINCT n FROM Note n
           LEFT JOIN FETCH n.category c
           WHERE n.user = :user
           """)
    List<Note> findByUserWithCategory(User user);

    // 특정 노트 조회 (Category fetch만)
    @Query("""
           SELECT DISTINCT n FROM Note n
           LEFT JOIN FETCH n.category c
           WHERE n.id = :id AND n.user = :user
           """)
    Optional<Note> findByIdAndUserWithCategory(Long id, User user);

    // 카테고리별 노트 조회 (Category fetch만)
    @Query("""
           SELECT DISTINCT n FROM Note n
           LEFT JOIN FETCH n.category c
           WHERE n.category = :category
           """)
    List<Note> findByCategoryWithFetch(Category category);

    // 관리자 유저 삭제용 - files까지 fetch
    @Query("""
           SELECT DISTINCT n FROM Note n
           LEFT JOIN FETCH n.files
           WHERE n.user = :user
           """)
    List<Note> findByUserWithFiles(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Note n WHERE n.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
