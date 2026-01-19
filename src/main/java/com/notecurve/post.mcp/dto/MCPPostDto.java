package com.notecurve.post.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class MCPPostDto {
    private Long id;
    private String title;
    private String subtitle;
    private String category;
    private String content;
    private LocalDate date;
}
