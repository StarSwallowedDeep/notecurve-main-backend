package com.notecurve.post.mcp.service;

import com.notecurve.post.domain.Post;
import com.notecurve.post.mcp.dto.MCPPostDto;
import com.notecurve.post.mcp.repository.MCPPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MCPPostService {

    private final MCPPostRepository mcpPostRepository;

    public List<MCPPostDto> getRecentPosts(String category, int limit) {
        List<Post> posts;

        if (category == null || category.isBlank()) {
            // category 없으면 전체 글 조회
            posts = mcpPostRepository.findAll(PageRequest.of(0, limit)).getContent();
        } else {
            // category 있으면 해당 카테고리만 조회
            posts = mcpPostRepository.findRecentPostsByCategory(
                    category, PageRequest.of(0, limit)
            );
        }

        return posts.stream()
                .map(post -> MCPPostDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .subtitle(post.getSubtitle())
                        .category(post.getCategory())
                        .content(post.getContent())
                        .date(post.getDate())
                        .build())
                .collect(Collectors.toList());
    }
}
