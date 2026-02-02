package com.notecurve.post.mcp.service;

import com.notecurve.post.domain.Post;
import com.notecurve.post.mcp.dto.MCPPostDto;
import com.notecurve.post.mcp.dto.MCPPostSummaryDto;
import com.notecurve.post.mcp.repository.MCPPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MCPPostService {

    private final MCPPostRepository mcpPostRepository;

    // 단일 글 조회
    public Optional<MCPPostDto> getPost(Long postId) {
        return mcpPostRepository.findById(postId)
                .map(post -> MCPPostDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .subtitle(post.getSubtitle())
                        .content(post.getContent())
                        .date(post.getDate())
                        .build());
    }

    // 검색용
    public List<MCPPostSummaryDto> searchPosts(String category, int limit) {
        List<Post> posts = mcpPostRepository.findPostsByCategory(category, PageRequest.of(0, limit));
        return posts.stream()
                .map(post -> MCPPostSummaryDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .subtitle(post.getSubtitle())
                        .date(post.getDate())
                        .build())
                .collect(Collectors.toList());
    }
}
