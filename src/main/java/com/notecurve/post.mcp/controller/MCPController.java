package com.notecurve.post.mcp.controller;

import com.notecurve.post.mcp.dto.MCPPostDto;
import com.notecurve.post.mcp.service.MCPPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mcp/posts")
@RequiredArgsConstructor
public class MCPController {

    private final MCPPostService mcpPostService;

    /**
     * AI가 호출할 글 데이터 조회 API
     * 예: /mcp/posts?category=tech&limit=10
     */
    @GetMapping
    public List<MCPPostDto> getRecentPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return mcpPostService.getRecentPosts(category, limit);
    }
}
