package com.notecurve.note.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;

import com.notecurve.category.domain.Category;
import com.notecurve.notefile.domain.NoteFile;
import com.notecurve.user.domain.User;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    private String title;
    
    @Setter
    @Lob
    private String content;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteFile> files;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
