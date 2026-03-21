package com.notecurve.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent {
    private String type;
    private Long commentId;
    private Long userId;
    private String content;
    private String userName;
    private Long messageBoardId;
    private String messageBoardTitle;
}
