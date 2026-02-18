package com.khademni.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.khademni.App;
import com.khademni.model.*;

import com.khademni.service.ArticleController;
import com.khademni.service.CommentaireController;
import com.khademni.service.FavoriController;
import com.khademni.utils.SessionManager;

import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;



public class ArticlePublicationController {

    @FXML private ListView<Article> listArticles;

    private final ArticleController articleService = new ArticleController();
    private final CommentaireController commentaireService = new CommentaireController();
    private final FavoriController favoriService = new FavoriController();
@FXML
private TilePane articlesContainer;

    @FXML
    public void initialize() {
        loadArticles();

    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadArticles() {
        try {
            var articlesVisibles = articleService.findAll().stream()
                    .filter(a -> "VISIBLE".equalsIgnoreCase(a.getStatus()))
                    .toList();

            articlesContainer.getChildren().clear();

for (Article article : articlesVisibles) {
    VBox card = createArticleCard(article);
    articlesContainer.getChildren().add(card);
}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
private VBox createArticleCard(Article article) {
    VBox commentairesBox = new VBox(10);
    commentairesBox.setVisible(false);
    commentairesBox.setManaged(false);

    // ===== IMAGE =====
    StackPane imageContainer = new StackPane();
    imageContainer.setPrefWidth(380);
    imageContainer.setPrefHeight(180);
    imageContainer.setStyle("-fx-border-color: #ddd; -fx-border-radius: 15; -fx-background-radius: 15; -fx-overflow: hidden;");

    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
    imageView.setFitWidth(380);
    imageView.setFitHeight(180);
    imageView.setPreserveRatio(true);
    imageView.setSmooth(true);
    imageView.setCache(true);

    try {
        String path = article.getImagePath();
        if (path != null && !path.isEmpty()) {
            javafx.scene.image.Image img = new javafx.scene.image.Image("file:" + path, false);
            imageView.setImage(img);
        } else {
            // image par défaut si aucun chemin
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResource("/images/default.png").toExternalForm());
            imageView.setImage(img);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    // ===== FAVORI ICON =====
    FontIcon heartIcon = new FontIcon(FontAwesomeSolid.HEART);
    heartIcon.setIconSize(30); // plus grand
    updateHeartColor(heartIcon, article);

    Label favoriCountLabel = new Label(String.valueOf(getFavoriCount(article)));
    favoriCountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");

    Button likeBtn = new Button();
    likeBtn.setGraphic(heartIcon);
    likeBtn.getStyleClass().add("btn-icon");
    likeBtn.setStyle("-fx-background-color: transparent;");

    likeBtn.setOnAction(e -> {
        toggleFavori(article);
        updateHeartColor(heartIcon, article);
        favoriCountLabel.setText(String.valueOf(getFavoriCount(article))); // Mettre à jour le compteur
    });

    HBox favoriBox = new HBox(5, likeBtn, favoriCountLabel);
    favoriBox.setAlignment(Pos.CENTER_LEFT);

    StackPane.setAlignment(favoriBox, Pos.TOP_RIGHT);
    StackPane.setMargin(favoriBox, new Insets(10));
    imageContainer.getChildren().addAll(imageView, favoriBox);

    // ===== TITRE =====
    Label title = new Label(article.getTitle());
    title.getStyleClass().add("card-title");

    // ===== CONTENU =====
    Label content = new Label(article.getContent());
    content.setWrapText(true);
    content.getStyleClass().add("card-content");

    // ===== DATE =====
    Label date = new Label("Publié le " + article.getCreatedAt());
    date.getStyleClass().add("card-date");

    // ===== COMMENT BUTTON avec compteur =====
    int nbCommentaires = 0;
    try {
        var commentaires = commentaireService.findAll().stream()
                .filter(c -> c.getArticle().getId().equals(article.getId()))
                .toList();

        nbCommentaires = commentaires.size(); // nombre de commentaires
    } catch (SQLException e1) {
        e1.printStackTrace();
    }

    Button commentBtn = new Button("💬 " + nbCommentaires);
    commentBtn.getStyleClass().add("btn-icon");

    commentBtn.setOnAction(e -> {
        boolean visible = !commentairesBox.isVisible();
        commentairesBox.setVisible(visible);
        commentairesBox.setManaged(visible);

        if (visible) {
            chargerCommentaires(article, commentairesBox);
        }
    });

    HBox actions = new HBox(10, commentBtn);
    actions.setAlignment(Pos.CENTER_LEFT);

    VBox card = new VBox(15, imageContainer, title, content, date, actions, commentairesBox);
    card.getStyleClass().add("article-card");
    card.setPrefWidth(400);

    return card;
}

private UserModel getCurrentUser() {
    // Replace with the actual logic to retrieve the logged-in user
    return SessionManager.getCurrentUser(); // Example: Using a SessionManager class
}



private void chargerCommentaires(Article article, VBox commentairesBox) {
    commentairesBox.getChildren().clear();

    try {
        var commentaires = commentaireService.findByArticleId(article.getId());
        UserModel currentUser = SessionManager.getCurrentUser();

        for (Commentaire c : commentaires) {
            // Author and time
            Label authorLabel = new Label(c.getUser().getFirstName() + " " + c.getUser().getLastName());
            authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6a1b9a;");

            Label timeLabel = new Label(formatTimeAgo(c.getCreatedAt()));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

            HBox authorTimeBox = new HBox(10, authorLabel, timeLabel);
            authorTimeBox.setAlignment(Pos.CENTER_LEFT);

            // Comment content
            Label contentLabel = new Label(c.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a148c;");

            VBox commentContentBox = new VBox(5, authorTimeBox, contentLabel);
            commentContentBox.setPadding(new Insets(5, 10, 5, 10));
            commentContentBox.setStyle("-fx-background-color: #f3e5f5; -fx-border-color: #ce93d8; -fx-border-radius: 5; -fx-background-radius: 5;");

            // Editable area for editing comments
            TextArea editArea = new TextArea(c.getContent());
            editArea.setWrapText(true);
            editArea.setVisible(false);
            editArea.setManaged(false);
            editArea.setStyle("-fx-border-color: #ce93d8; -fx-border-radius: 5; -fx-background-radius: 5;");

            Button saveBtn = new Button("Save");
            saveBtn.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-size: 12px; -fx-border-radius: 5; -fx-background-radius: 5;");
            saveBtn.setVisible(false);
            saveBtn.setManaged(false);

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-font-size: 12px; -fx-border-radius: 5; -fx-background-radius: 5;");
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);

            // Buttons for editing and deleting
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-size: 12px; -fx-border-radius: 5; -fx-background-radius: 5;");

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 12px; -fx-border-radius: 5; -fx-background-radius: 5;");

            // Show buttons only if the current user is the owner
            if (currentUser != null && c.getUser().getId() == currentUser.getId()) {
                editBtn.setOnAction(ev -> {
                    contentLabel.setVisible(false);
                    contentLabel.setManaged(false);
                    editArea.setVisible(true);
                    editArea.setManaged(true);
                    saveBtn.setVisible(true);
                    saveBtn.setManaged(true);
                    cancelBtn.setVisible(true);
                    cancelBtn.setManaged(true);
                    editBtn.setVisible(false);
                    editBtn.setManaged(false);
                });

                saveBtn.setOnAction(ev -> {
                    String newContent = editArea.getText().trim();
                    int wordCount = newContent.isEmpty() ? 0 : newContent.split("\\s+").length;

                    if (wordCount < 3 || wordCount > 30) {
                        showAlert(Alert.AlertType.WARNING, "Error", "The comment must contain between 3 and 30 words.");
                        return;
                    }

                    try {
                        c.setContent(newContent);
                        commentaireService.update(c);
                        chargerCommentaires(article, commentairesBox);
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Comment updated successfully!");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to update comment.");
                    }
                });

                cancelBtn.setOnAction(ev -> {
                    editArea.setVisible(false);
                    editArea.setManaged(false);
                    saveBtn.setVisible(false);
                    saveBtn.setManaged(false);
                    cancelBtn.setVisible(false);
                    cancelBtn.setManaged(false);
                    contentLabel.setVisible(true);
                    contentLabel.setManaged(true);
                    editBtn.setVisible(true);
                    editBtn.setManaged(true);
                });

                deleteBtn.setOnAction(ev -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this comment?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            try {
                                commentaireService.delete(c.getId());
                                chargerCommentaires(article, commentairesBox);
                            } catch (SQLException e) {
                                e.printStackTrace();
                                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete comment.");
                            }
                        }
                    });
                });

                HBox buttonBox = new HBox(10, editBtn, deleteBtn, saveBtn, cancelBtn);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                commentContentBox.getChildren().addAll(editArea, buttonBox);
            }

            commentairesBox.getChildren().add(commentContentBox);
        }

        // ===== Input Field for Adding Comments =====
        TextArea newCommentArea = new TextArea();
        newCommentArea.setPromptText("Write a comment...");
        newCommentArea.setWrapText(true);
        newCommentArea.setPrefRowCount(2);
        newCommentArea.setStyle("-fx-border-color: #ce93d8; -fx-border-radius: 5; -fx-background-radius: 5;");

        Button addCommentBtn = new Button("Add Comment");
        addCommentBtn.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 5; -fx-background-radius: 5;");

        addCommentBtn.setOnAction(ev -> {
            String content = newCommentArea.getText().trim();
            int wordCount = content.isEmpty() ? 0 : content.split("\\s+").length;

            if (wordCount < 3 || wordCount > 30) {
                showAlert(Alert.AlertType.WARNING, "Error", "The comment must contain between 3 and 30 words.");
                return;
            }

            try {
                if (currentUser == null) {
                    throw new IllegalStateException("No user is currently logged in.");
                }

                Commentaire newComment = new Commentaire(
                        null,
                        content,
                        "VISIBLE",
                        LocalDateTime.now(),
                        article,
                        currentUser
                );

                commentaireService.create(newComment);
                newCommentArea.clear();
                chargerCommentaires(article, commentairesBox);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Comment added successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add comment.");
            }
        });

        HBox inputBox = new HBox(10, newCommentArea, addCommentBtn);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setPadding(new Insets(10, 0, 0, 0));

        commentairesBox.getChildren().add(inputBox);

    } catch (SQLException e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error", "Failed to load comments.");
    }
}



private void deleteComment(Commentaire commentaire, VBox commentairesBox) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Delete Comment");
    alert.setHeaderText("Are you sure you want to delete this comment?");
    alert.setContentText("This action cannot be undone.");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try {
            commentaireService.delete(commentaire.getId());
            commentairesBox.getChildren().removeIf(node -> {
                HBox commentBox = (HBox) node;
                Label contentLabel = (Label) commentBox.getChildren().get(0);
                return contentLabel.getText().equals(commentaire.getContent());
            });
            showAlert(Alert.AlertType.INFORMATION, "Success", "Comment deleted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete comment.");
        }
    }
}
private void editComment(Commentaire commentaire) {
    TextInputDialog dialog = new TextInputDialog(commentaire.getContent());
    dialog.setTitle("Edit Comment");
    dialog.setHeaderText("Modify your comment:");
    dialog.setContentText("Comment:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(newContent -> {
        try {
            commentaire.setContent(newContent);
            commentaireService.update(commentaire);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Comment updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update comment.");
        }
    });
}
private int getFavoriCount(Article article) {
    try {
        return (int) favoriService.findAll()
                .stream()
                .filter(favori -> favori.getArticle().getId().equals(article.getId()))
                .count();
    } catch (SQLException e) {
        e.printStackTrace();
        return 0;
    }
}
private boolean isFavori(Article article) {
    try {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Vérifier si l'article est dans les favoris de l'utilisateur connecté
        return favoriService.findByUserId((long) currentUser.getId())
                .stream()
                .anyMatch(favori -> favori.getArticle().getId().equals(article.getId()));
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
private String formatTimeAgo(LocalDateTime createdAt) {
    LocalDateTime now = LocalDateTime.now();
    long minutes = java.time.Duration.between(createdAt, now).toMinutes();
    long hours = java.time.Duration.between(createdAt, now).toHours();
    long days = java.time.Duration.between(createdAt, now).toDays();

    if (minutes < 1) return "just now";
    if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
    if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
    return days + (days == 1 ? " day ago" : " days ago");
}





private void updateHeartColor(FontIcon icon, Article article) {
    if (isFavori(article)) {
        icon.setIconColor(javafx.scene.paint.Color.RED);
    } else {
        icon.setIconColor(javafx.scene.paint.Color.GRAY);
    }
}

private void toggleFavori(Article article) {
    try {
        var exist = favoriService.findAll().stream()
                .filter(f -> f.getArticleId().equals(article.getId()))
                .findFirst();

        if (exist.isPresent()) {
            favoriService.delete(exist.get().getId());
        } else {
            Favori f = new Favori(null, 1L, article, null);
            favoriService.create(f);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}


}


