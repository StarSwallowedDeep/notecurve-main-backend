package com.notecurve.itletter.repository;

import com.notecurve.itletter.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findTopByCategoryOrderByIdDesc(String category);
}
