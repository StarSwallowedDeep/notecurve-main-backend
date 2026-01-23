package com.notecurve.itletter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ArticleDTO {

    private Long id;
    private String title;
    private String content;
    private String source;
    private LocalDateTime date;
}
