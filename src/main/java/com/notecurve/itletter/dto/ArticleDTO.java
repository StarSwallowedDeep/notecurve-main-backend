package com.notecurve.itletter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ArticleDTO {

    private Long id;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotBlank(message = "Source cannot be blank")
    private String source;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    private LocalDateTime date;
}
