package com.notecurve.messageboard.domain;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;

import com.notecurve.user.domain.User;

@Entity
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "message_board_id")
    @JsonBackReference
    private MessageBoard messageBoard;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
