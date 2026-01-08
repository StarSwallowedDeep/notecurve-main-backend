package com.notecurve.messageboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageBoardDTO {

    private Long id;
    private String title;
    private String createdAt;
    private List<CommentDTO> comments;
    private String userName;
}
