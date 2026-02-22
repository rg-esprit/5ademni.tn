package com.khademni.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import com.khademni.App;
import com.khademni.model.*;

import com.khademni.service.ArticleController;
import com.khademni.service.CommentaireController;
import com.khademni.service.FavoriController;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.SessionManager;

import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
private TilePane gigsContainer;
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
            // Charger uniquement les articles avec le statut "VISIBLE"
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les articles.");
        }
    }
private VBox createArticleCard(Article article) {

    // ===== CARTE PRINCIPALE =====
    VBox card = new VBox(15);
    card.setAlignment(Pos.CENTER_LEFT);
    card.setStyle("""
        -fx-background-color: white;
        -fx-padding: 20;
        -fx-background-radius: 20;
        -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);
    """);
    card.setPrefWidth(900);

    // ================= IMAGE (GAUCHE) =================
    ImageView imageView = new ImageView();
    imageView.setFitWidth(150);
    imageView.setFitHeight(150);
    imageView.setPreserveRatio(true);

    try {
        String path = article.getImagePath();
        if (path != null && !path.isEmpty()) {
            imageView.setImage(new Image("file:" + path));
        } else {
            imageView.setImage(new Image(getClass()
                    .getResource("/images/default.png").toExternalForm()));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    // ================= CONTENU CENTRE =================
    VBox contentBox = new VBox(8);
    contentBox.setAlignment(Pos.CENTER_LEFT);
    contentBox.setPrefWidth(500);

    Label title = new Label(article.getTitle());
    title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    Label description = new Label(article.getContent());
    description.setWrapText(true);
    description.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

    Label date = new Label("Publié le " + article.getCreatedAt());
    date.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

    contentBox.getChildren().addAll(title, description, date);

    // ================= ACTIONS DROITE =================
    VBox actionsBox = new VBox(20);
    actionsBox.setAlignment(Pos.CENTER);

    // ===== FAVORI =====
    FontIcon heartIcon = new FontIcon(FontAwesomeSolid.HEART);
    heartIcon.setIconSize(22);
    updateHeartColor(heartIcon, article);

    Label favoriCount = new Label(String.valueOf(getFavoriCount(article)));

    Button likeBtn = new Button();
    likeBtn.setGraphic(heartIcon);
    likeBtn.setStyle("-fx-background-color: transparent;");

    likeBtn.setOnAction(e -> {
        toggleFavori(article);
        updateHeartColor(heartIcon, article);
        favoriCount.setText(String.valueOf(getFavoriCount(article)));
    });

    HBox favoriBox = new HBox(5, likeBtn, favoriCount);
    favoriBox.setAlignment(Pos.CENTER);

    // ===== BOUTON Voir Gigs =====
    
    Button voirArticleBtn = new Button("Voir Gigs");
    voirArticleBtn.getStyleClass().add("button-modern");
    voirArticleBtn.setStyle("""
             -fx-background-color: linear-gradient(to right, #7c3aed, #6c0df2);
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.3), 10, 0, 0, 4);
    """);
 voirArticleBtn.setOnMouseEntered(e -> voirArticleBtn.setStyle("""
        -fx-background-color: linear-gradient(to right, #5f0ad6, #5209be);
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.5), 12, 0, 0, 6);
    """));

    voirArticleBtn.setOnMouseExited(e -> voirArticleBtn.setStyle("""
        -fx-background-color: linear-gradient(to right, #7c3aed, #6c0df2);
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.3), 10, 0, 0, 4);
    """));

    voirArticleBtn.setOnAction(e -> navigateToGigsPage(article));

    // ===== COMMENTAIRE =====
    FontIcon commentIcon = new FontIcon(FontAwesomeSolid.COMMENT);
    commentIcon.setIconSize(20);

    Label commentCount = new Label(String.valueOf(getCommentCount(article)));

    Button commentBtn = new Button();
    commentBtn.setGraphic(commentIcon);
    commentBtn.setStyle("-fx-background-color: transparent;");

    HBox commentBox = new HBox(5, commentBtn, commentCount);
    commentBox.setAlignment(Pos.CENTER);

    actionsBox.getChildren().addAll(favoriBox, voirArticleBtn, commentBox);

    // ================= LAYOUT PRINCIPAL =================
    HBox mainRow = new HBox(30, imageView, contentBox, actionsBox);
    mainRow.setAlignment(Pos.CENTER_LEFT);

    // ================= COMMENTAIRES (EN DESSOUS) =================
    VBox commentairesBox = new VBox(10);
    commentairesBox.setVisible(false);
    commentairesBox.setManaged(false);

    commentBtn.setOnAction(e -> {
        boolean visible = !commentairesBox.isVisible();
        commentairesBox.setVisible(visible);
        commentairesBox.setManaged(visible);

         if (visible) {
            card.setStyle("""
                -fx-background-color: white;
                -fx-padding: 30;
                -fx-background-radius: 20;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);
            """);
            chargerCommentaires(article, commentairesBox);
        } else {
            card.setStyle("""
                -fx-background-color: white;
                -fx-padding: 20;
                -fx-background-radius: 20;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5);
            """);
        }
    });

    card.getChildren().addAll(mainRow, commentairesBox);

    return card;
}




private int getCommentCount(Article article) {
    try {
        return (int) commentaireService.findAll()
                .stream()
                .filter(comment -> comment.getArticle().getId().equals(article.getId()))
                .count();
    } catch (SQLException e) {
        e.printStackTrace();
        return 0;
    }
}

private void navigateToGigsPage(Article article) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Article/GigPage.fxml"));
        Parent gigsPage = loader.load();

        // Passer l'article sélectionné au contrôleur de la page des gigs
        GigDisplayController gigDisplayController = loader.getController();
        gigDisplayController.setArticle(article);

        // Afficher la nouvelle page
        Stage stage = (Stage) articlesContainer.getScene().getWindow();
        stage.setScene(new Scene(gigsPage));
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page des gigs.");
    }
}

public void loadGigsByArticle(Article article) {
    try {
        // Extraire les mots-clés de l'article
        List<String> keywords = extractKeywords(article);

        // Rechercher les Gigs correspondants dans la base de données
        List<GigModel> gigs = findGigsByKeywords(keywords);

        // Vider le conteneur avant d'ajouter les nouveaux Gigs
        gigsContainer.getChildren().clear();

        if (gigs.isEmpty()) {
            Label noGigsLabel = new Label("Aucun Gig trouvé pour cet article.");
            noGigsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            gigsContainer.getChildren().add(noGigsLabel);
        } else {
            for (GigModel gig : gigs) {
                VBox gigCard = createGigCard(gig);
                gigsContainer.getChildren().add(gigCard);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les Gigs.");
    }
}

private void loadGigsForArticle(Article article) {
    try {
        // Extraire les mots-clés de l'article
        List<String> keywords = extractKeywords(article);

        // Rechercher les Gigs correspondants dans la base de données
        List<GigModel> gigs = findGigsByKeywords(keywords);

        // Créer une nouvelle fenêtre pour afficher les Gigs
        Stage stage = new Stage();
        VBox gigsContainer = new VBox(10);
        gigsContainer.setPadding(new Insets(10));

        if (gigs.isEmpty()) {
            Label noGigsLabel = new Label("Aucun Gig trouvé pour cet article.");
            noGigsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            gigsContainer.getChildren().add(noGigsLabel);

            // Suggérer des Gigs populaires
            List<GigModel> popularGigs = findPopularGigs();
            if (!popularGigs.isEmpty()) {
                Label suggestionLabel = new Label("Suggestions de Gigs populaires :");
                suggestionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                gigsContainer.getChildren().add(suggestionLabel);

                for (GigModel gig : popularGigs) {
                    VBox gigCard = createGigCard(gig);
                    gigsContainer.getChildren().add(gigCard);
                }
            }
        } else {
            for (GigModel gig : gigs) {
                VBox gigCard = createGigCard(gig);
                gigsContainer.getChildren().add(gigCard);
            }
        }

        ScrollPane scrollPane = new ScrollPane(gigsContainer);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 400, 600);
        stage.setScene(scene);
        stage.setTitle("Liste des Gigs");
        stage.show();
    } catch (Exception e) {
        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les Gigs : " + e.getMessage());
    }
}

private List<GigModel> findPopularGigs() throws SQLException {
    List<GigModel> gigs = new ArrayList<>();

    String query = """
        SELECT g.id, g.title, g.description, g.price, g.delivery_time, g.image, g.status
        FROM gig g
        ORDER BY g.delivery_time DESC
        LIMIT 5
    """;

    try (Connection conn = MyDataBase.getConnection();
         PreparedStatement ps = conn.prepareStatement(query)) {

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            GigModel gig = new GigModel(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getTimestamp("delivery_time").toLocalDateTime(),
                rs.getString("image"),
                rs.getString("status")
            );
            gigs.add(gig);
        }
    }

    return gigs;
}

private VBox createGigCard(GigModel gig) {
    VBox card = new VBox(10);
    card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 10; -fx-padding: 10;");

    // Titre du Gig
    Label gigTitle = new Label(gig.getTitle());
    gigTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

    // Description du Gig
    Label gigDescription = new Label(gig.getDescription());
    gigDescription.setWrapText(true);
    gigDescription.setStyle("-fx-font-size: 14px;");

    // Prix du Gig
    Label gigPrice = new Label(String.format("Prix : %.2f TND", gig.getPrice()));
    gigPrice.setStyle("-fx-font-size: 14px; -fx-text-fill: #4caf50;");

    // Ajouter les éléments à la carte
    card.getChildren().addAll(gigTitle, gigDescription, gigPrice);

    return card;
}


private List<GigModel> findGigsByKeywords(List<String> keywords) throws SQLException {
    List<GigModel> gigs = new ArrayList<>();

    // Construire une requête SQL dynamique avec des mots-clés
    StringBuilder queryBuilder = new StringBuilder("""
        SELECT g.id, g.title, g.description, g.price, g.delivery_time, g.image, g.status
        FROM gig g
        WHERE
    """);

    for (int i = 0; i < keywords.size(); i++) {
        queryBuilder.append("LOWER(g.title) LIKE ? OR LOWER(g.description) LIKE ?");
        if (i < keywords.size() - 1) {
            queryBuilder.append(" OR ");
        }
    }

    try (Connection conn = MyDataBase.getConnection();
         PreparedStatement ps = conn.prepareStatement(queryBuilder.toString())) {

        // Ajouter les mots-clés aux paramètres de la requête
        int paramIndex = 1;
        for (String keyword : keywords) {
            String likePattern = "%" + keyword + "%";
            ps.setString(paramIndex++, likePattern);
            ps.setString(paramIndex++, likePattern);
        }

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            GigModel gig = new GigModel(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getTimestamp("delivery_time").toLocalDateTime(),
                rs.getString("image"),
                rs.getString("status")
            );

            // Calculer le score de pertinence
            double relevanceScore = calculateRelevanceScore(gig, keywords);
            gig.setRelevanceScore(relevanceScore); // Ajoutez un champ `relevanceScore` dans `GigModel`

            gigs.add(gig);
        }
    }

    // Trier les Gigs par pertinence (score décroissant)
    gigs.sort((g1, g2) -> Double.compare(g2.getRelevanceScore(), g1.getRelevanceScore()));

    return gigs;
}

private double calculateRelevanceScore(GigModel gig, List<String> keywords) {
    double score = 0.0;

    // Convertir le titre et la description en minuscules
    String title = gig.getTitle().toLowerCase();
    String description = gig.getDescription().toLowerCase();

    // Calculer le score en fonction des mots-clés
    for (String keyword : keywords) {
        if (title.contains(keyword)) {
            score += 2.0; // Le titre a un poids plus élevé
        }
        if (description.contains(keyword)) {
            score += 1.0; // La description a un poids plus faible
        }
    }

    return score;
}


private List<String> extractKeywords(Article article) {
    List<String> keywords = new ArrayList<>();

    // Ajouter les mots du titre
    if (article.getTitle() != null) {
        String[] titleWords = article.getTitle().toLowerCase().split("\\s+");
        keywords.addAll(List.of(titleWords));
    }

    // Ajouter les mots de la description
    if (article.getContent() != null) {
        String[] contentWords = article.getContent().toLowerCase().split("\\s+");
        keywords.addAll(List.of(contentWords));
    }

    // Supprimer les doublons
    return keywords.stream().distinct().toList();
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
        UserModel currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Vous devez être connecté pour ajouter un favori.");
            return;
        }

        Favori favori = new Favori(currentUser.getId(), article.getId());
        if (favoriService.isFavori(favori)) {
            favoriService.delete(favori.getId());
        } else {
            favoriService.create(favori);
        }
    } catch (SQLException e) {
        e.printStackTrace();
       showAlert(Alert.AlertType.WARNING, "Erreur", "Une erreur s'est produite.");
    } catch (Exception e) {
        e.printStackTrace();
       showAlert(Alert.AlertType.WARNING, "Erreur", "Une erreur s'est produite.");   }
}

}