package com.notecurve.itletter.controller;

import com.notecurve.itletter.dto.ArticleDTO;
import com.notecurve.itletter.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService service;

    @PostMapping
    public ResponseEntity<ArticleDTO> createArticle(@Valid @RequestBody ArticleDTO articleDTO) {
        ArticleDTO createdArticleDTO = service.createArticle(articleDTO);
        return new ResponseEntity<>(createdArticleDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public List<ArticleDTO> getAllArticles() {
        return service.getAllArticles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDTO> getArticleById(@PathVariable Long id) {
        return service.getArticleById(id)
                .map(articleDTO -> new ResponseEntity<>(articleDTO, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
