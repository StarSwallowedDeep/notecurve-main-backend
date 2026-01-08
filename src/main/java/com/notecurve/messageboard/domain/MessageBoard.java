package com.notecurve.messageboard.domain;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

import com.notecurve.user.domain.User;
import com.notecurve.messageboard.dto.CommentDTO;

@Entity
@Getter
@Setter
public class MessageBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "messageBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Comment> comments = new ArrayList<>();

    public String getFormattedCreatedAt() {
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime zonedDateTime = createdAt.atZone(ZoneId.of("UTC")).withZoneSameInstant(seoulZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm:ss");
        return zonedDateTime.format(formatter);
    }
}
