package com.notecurve.notefile.repository;

import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.note.domain.Note;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NoteFileRepository extends JpaRepository<NoteFile, Long> {

    // Note와 NoteFile을 한 번에 가져오기
    @EntityGraph(attributePaths = {"note"})
    List<NoteFile> findByNote(Note note);

    // NoteFile을 Note와 User와 함께 가져오기
    @EntityGraph(attributePaths = {"note", "note.user"})
    Optional<NoteFile> findWithNoteAndUserById(Long fileId);

    // 파일 이름으로 조회 (이미지 서빙용)
    @EntityGraph(attributePaths = {"note", "note.user"})
    Optional<NoteFile> findByStoredNameAndNote_UserId(String storedName, Long userId);

    Optional<NoteFile> findByStoredName(String storedName);

    @Modifying
    @Transactional
    @Query("DELETE FROM NoteFile nf WHERE nf.note.user.id = :userId")
    void deleteByNoteUserId(@Param("userId") Long userId);
}
