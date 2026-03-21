package com.notecurve.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageBoardEvent {
    private String type;
    private Long boardId;
    private Long userId;
    private String title;
    private String userName;
    private String createdAt;
}
