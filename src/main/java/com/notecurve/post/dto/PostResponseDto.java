package com.notecurve.post.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponseDto {

    private Long id;
    private String title;
    private String subtitle;
    private String category;
    private String content;
    private String thumbnailImageUrl;
    private List<String> contentImageUrls;
    private LocalDate date;
    private String userName;
    private Long userId;
    private String profileImage;
}
