package com.khademni.model;

import java.time.LocalDateTime;

public class Favori {
    private Long id;
    private int userId;
    private Article article;
    private LocalDateTime createdAt;

    public Favori() {
    }

public Favori(Long id, int userId, Article article, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.article = article;
    this.createdAt = createdAt;
}

public Favori(int userId, Long articleId) {
    this.userId = userId;
    this.article = new Article();
    this.article.setId(articleId);
}
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public Long getArticleId() {
        return article != null ? article.getId() : null;
    }
public LocalDateTime getCreatedAt() {
    return createdAt;
}

public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}
    @Override
    public String toString() {
        return "Favori{" +
                "id=" + id +
                ", userId=" + userId +
                ", articleId=" + (article != null ? article.getId() : null) +
                '}';
    }
}
