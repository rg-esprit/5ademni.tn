package com.khademni.model;

import java.time.LocalDateTime;
import java.util.List;

public class Article {

    private Long id;

    private String title;

    private String content;

    private String status;

    private LocalDateTime createdAt;
private String imagePath; 
    private List<Commentaire> commentaires;

    public Article() {

    }

public Article(Long id, String title, String content, String status, LocalDateTime createdAt, String imagePath) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.status = status;
    this.createdAt = createdAt;
    this.imagePath = imagePath;
}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getImagePath() {
    return imagePath;
}

public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
}

    public List<Commentaire> getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires;
    }
}
