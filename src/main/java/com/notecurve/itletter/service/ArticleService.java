package com.notecurve.itletter.service;

import com.notecurve.itletter.domain.Article;
import com.notecurve.itletter.dto.ArticleDTO;
import com.notecurve.itletter.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository repository;

    public ArticleDTO createArticle(ArticleDTO articleDTO) {
        Article article = Article.builder()
                .title(articleDTO.getTitle())
                .content(articleDTO.getContent())
                .source(articleDTO.getSource())
                .date(articleDTO.getDate())
                .build();

        return convertToDTO(repository.save(article));
    }

    public List<ArticleDTO> getAllArticles() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<ArticleDTO> getArticleById(Long id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    private ArticleDTO convertToDTO(Article article) {
        return new ArticleDTO(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getSource(),
                article.getDate()
        );
    }
}
