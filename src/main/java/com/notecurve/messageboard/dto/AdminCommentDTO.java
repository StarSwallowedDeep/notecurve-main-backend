package com.notecurve.messageboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminCommentDTO {
    private Long id;
    private Long userId;
    private String content;
    private String userName;
    private Long messageBoardId;
    private String messageBoardTitle;
}
