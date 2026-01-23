package com.notecurve.itletter.repository;

import com.notecurve.itletter.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
