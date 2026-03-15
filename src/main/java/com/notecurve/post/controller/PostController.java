package com.notecurve.post.controller;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.notecurve.auth.security.UserDetailsImpl;
import com.notecurve.post.dto.PostRequestDto;
import com.notecurve.post.dto.PostResponseDto;
import com.notecurve.post.service.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 저장
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestBody @Valid PostRequestDto postRequestDto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        return ResponseEntity.ok(postService.savePost(postRequestDto, getCurrentUserId(), idempotencyKey));
    }

    // 게시글 ID로 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    // 게시글 전체 조회
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        List<PostResponseDto> posts = postService.getAllPosts();
        return posts.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(posts);
    }

    // 게시글 수정
    @PatchMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long id,
            @RequestBody PostRequestDto postRequestDto) throws IOException {
        return ResponseEntity.ok(postService.updatePost(id, postRequestDto, getCurrentUserId()));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id) {
        postService.deletePost(id, getCurrentUserId());
        return ResponseEntity.ok("게시글이 삭제되었습니다.");
    }

    // 현재 로그인 사용자 ID 조회
    private Long getCurrentUserId() {
        return ((UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal())
                .getUser()
                .getId();
    }
}
