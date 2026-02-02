package com.notecurve.post.mcp.controller;

import com.notecurve.post.mcp.dto.MCPPostDto;
import com.notecurve.post.mcp.dto.MCPPostSummaryDto;
import com.notecurve.post.mcp.service.MCPPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/mcp/posts")
@RequiredArgsConstructor
public class MCPController {

    private final MCPPostService mcpPostService;

    // get_post: 단일 글 조회
    @GetMapping("/{postId}")
    public Optional<MCPPostDto> getPost(@PathVariable Long postId) {
        return mcpPostService.getPost(postId);
    }

    // search_posts: 조건 검색
    @GetMapping
    public List<MCPPostSummaryDto> searchPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return mcpPostService.searchPosts(category, limit);
    }
}
