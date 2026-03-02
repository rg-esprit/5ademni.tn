package com.khademni.model;

import java.time.LocalDateTime;

public class Commentaire {
    private Long id;


    private String content;

    private String status; // PENDING, APPROVED, BLOCKED

    private LocalDateTime createdAt;
private UserModel user;


    private Article article;

    public Commentaire() {
    }

 public Commentaire(Long id, String content, String status, 
                    LocalDateTime createdAt, Article article, UserModel user) {
    this.id = id;
    this.content = content;
    this.status = status;
    this.createdAt = createdAt;
    this.article = article;
    this.user = user;
}

public UserModel getUser() {
    return user;
}

public void setUser(UserModel user) {
    this.user = user;
}

public Integer getUserId() {
    return user != null ? user.getId() : null;
}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", articleId=" + (article != null ? article.getId() : null) +
                '}';
    }
}
