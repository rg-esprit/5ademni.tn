package com.khademni.controller;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.json.JSONArray;
import org.json.JSONObject;
import com.khademni.App;
import com.khademni.model.*;
import com.khademni.utils.ContentModeration;
import com.khademni.service.ArticleController;
import com.khademni.service.CommentaireController;
import com.khademni.service.FavoriController;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.SessionManager;

import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
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

    @FXML
    private VBox commentCard;

    @FXML
    private VBox commentContainer;
    @FXML
    private TilePane gigsContainer;
    @FXML
    private ListView<Article> listArticles;

    private final ArticleController articleService = new ArticleController();
    private final CommentaireController commentaireService = new CommentaireController();
    private final FavoriController favoriService = new FavoriController();
    @FXML
    private TilePane articlesContainer;
    @FXML
    private HBox mainContainer;
    @FXML
    private StackPane rootPane;

    @FXML
    private VBox mainContent;

    @FXML
    public void initialize() {
        loadArticles();
    }

    @FXML
    private VBox gigsCard;

    @FXML
    private void showGigsCard() {
        gigsCard.setVisible(true);
        gigsCard.setManaged(true);

        // Animation de translation depuis la droite
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(300), gigsCard);
        translateTransition.setFromX(300); // Position initiale (en dehors de l'écran)
        translateTransition.setToX(0); // Position finale (visible à l'écran)
        translateTransition.setInterpolator(Interpolator.EASE_OUT);
        translateTransition.play();
    }

    @FXML
    private void showCommentCard(Article article) {
        commentCard.setVisible(true);
        commentCard.setManaged(true);
        mainContainer.setDisable(true); // Disable the main content
        mainContainer.setStyle("-fx-opacity: 0.3;"); // Dim the main content
        chargerCommentaires(article, commentContainer); // Load comments for the selected article
        // Center the comment card
        StackPane.setAlignment(commentCard, Pos.CENTER);
    }

    @FXML
    private void hideCommentCard() {
        commentCard.setVisible(false);
        commentCard.setManaged(false);
        mainContainer.setDisable(false); // Re-enable the main content
        mainContainer.setStyle("-fx-opacity: 1;"); // Restore the main content opacity
    }

    @FXML
    private void showGigsCard(Article article) {
        gigsCard.setVisible(true);
        gigsCard.setManaged(true);
        mainContainer.getStyleClass().add("gigs-visible"); // Add the class to align left
        loadGigsByArticle(article); // Load Gigs for the selected article
    }

    @FXML
    private void hideGigsCard() {
        gigsCard.setVisible(false);
        gigsCard.setManaged(false);
        mainContainer.getStyleClass().remove("gigs-visible"); // Retirer la classe pour centrer
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showModernDialog(String title, String message, String type) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        VBox dialogVBox = new VBox(20);
        dialogVBox.setAlignment(Pos.CENTER);
        dialogVBox.setPadding(new Insets(20));
        dialogVBox.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-weight: bold;");

        Button btnClose = new Button("OK");
        btnClose.setStyle(
                "-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 20;");
        btnClose.setOnAction(e -> dialog.close());

        dialogVBox.getChildren().addAll(lblMessage, btnClose);

        Scene dialogScene = new Scene(dialogVBox, 300, 150);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private void loadArticles() {
        try {
            // Charger uniquement les articles avec le statut "VISIBLE"
            var articlesVisibles = articleService.findAll().stream()
                    .filter(a -> "VISIBLE".equalsIgnoreCase(a.getStatus()))
                    .toList();

            // Appliquer le tri en fonction du filtre sélectionné
            var articlesTries = articlesVisibles.stream()
                    .sorted((a1, a2) -> {
                        if ("favoris".equals(currentFilter)) {
                            return Integer.compare(getFavoriCount(a2), getFavoriCount(a1)); // Trier par favoris
                        } else if ("commentaires".equals(currentFilter)) {
                            return Integer.compare(getCommentCount(a2), getCommentCount(a1)); // Trier par commentaires
                        }
                        return 0; // Aucun tri par défaut
                    })
                    .toList();

            articlesContainer.getChildren().clear();

            for (Article article : articlesTries) {
                VBox card = createArticleCard(article);
                articlesContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les articles.");
        }

    }

    @FXML
    private Button filterButton;

    private String currentFilter = "favoris";

    @FXML
    private void showFilterOptions() {
        ContextMenu filterMenu = new ContextMenu();

        MenuItem filterByFavoris = new MenuItem("Filtrer par Favoris");
        filterByFavoris.setOnAction(e -> {
            currentFilter = "favoris";
            loadArticles(); // Recharger les articles avec le filtre sélectionné
        });

        MenuItem filterByCommentaires = new MenuItem("Filtrer par Commentaires");
        filterByCommentaires.setOnAction(e -> {
            currentFilter = "commentaires";
            loadArticles(); // Recharger les articles avec le filtre sélectionné
        });

        filterMenu.getItems().addAll(filterByFavoris, filterByCommentaires);
        filterMenu.show(filterButton, filterButton.getLayoutX(), filterButton.getLayoutY() + filterButton.getHeight());
    }

    private VBox commentairesBox = new VBox(10);

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

        voirArticleBtn.setOnAction(e -> showGigsCard(article));

        // ===== COMMENTAIRE =====
        FontIcon commentIcon = new FontIcon(FontAwesomeSolid.COMMENT);
        commentIcon.setIconSize(20);

        // Fetch the comment count for the article
        Label commentCount = new Label(String.valueOf(getCommentCount(article)));
        commentCount.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;"); // Optional styling for the count

        Button commentBtn = new Button();
        commentBtn.setGraphic(commentIcon);
        commentBtn.setStyle("-fx-background-color: transparent;");

        // Add the comment icon and count to an HBox
        HBox commentBox = new HBox(5, commentBtn, commentCount);
        commentBox.setAlignment(Pos.CENTER);

        // Set the action for the comment button
        commentBtn.setOnAction(e -> showCommentCard(article));

        // Add the commentBox to the actionsBox
        actionsBox.getChildren().addAll(favoriBox, voirArticleBtn, commentBox);

        // ================= LAYOUT PRINCIPAL =================
        HBox mainRow = new HBox(30, imageView, contentBox, actionsBox);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // ================= COMMENTAIRES (EN DESSOUS) =================
        VBox commentairesBox = new VBox(10);
        commentairesBox.setVisible(false);
        commentairesBox.setManaged(false);

        // Set the action for the comment button
        commentBtn.setOnAction(e -> showCommentCard(article));

        card.getChildren().addAll(mainRow, commentairesBox);

        return card;
    }
@FXML
private TextArea commentTextArea; // Ensure this is defined in your FXML file

  
@FXML
private void ajouterCommentaire() {
    System.out.println("Debug: Entered ajouterCommentaire method");

    // Récupérer le contenu du commentaire depuis le TextArea
    String contenuCommentaire = commentTextArea.getText();
    System.out.println("Debug: Comment content: " + contenuCommentaire);

    if (contenuCommentaire == null || contenuCommentaire.isBlank()) {
        System.out.println("Debug: Comment is empty or blank.");
        showAlert(Alert.AlertType.WARNING, "Erreur", "Le commentaire ne peut pas être vide.");
        return;
    }

    // Récupérer l'article sélectionné
    Article article = listArticles.getSelectionModel().getSelectedItem();
    if (article == null) {
        System.out.println("Debug: No article selected.");
        showAlert(Alert.AlertType.WARNING, "Erreur", "Aucun article sélectionné.");
        return;
    }

    System.out.println("Debug: Checking if comment is acceptable...");
    boolean isAcceptable = ContentModeration.isCommentAcceptable(contenuCommentaire);

    if (!isAcceptable) {
        System.out.println("Debug: Comment is not acceptable.");
        showAlert(Alert.AlertType.ERROR, "Commentaire rejeté", "Votre commentaire contient des propos inappropriés.");
        return;
    }

    System.out.println("Debug: Comment is acceptable. Proceeding to add comment...");
    try {
        Commentaire newComment = new Commentaire(
                null,
                contenuCommentaire,
                "VISIBLE",
                LocalDateTime.now(),
                article,
                SessionManager.getCurrentUser());

        commentaireService.create(newComment);

        System.out.println("Commentaire ajouté avec succès : " + contenuCommentaire);

        // Recharger les commentaires après l'ajout
        chargerCommentaires(article, commentContainer);

        showAlert(Alert.AlertType.INFORMATION, "Succès", "Commentaire ajouté avec succès.");
    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le commentaire.");
    }
}

    private int getCommentCount(Article article) {
        try {
            return commentaireService.countByArticle(article.getId());
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

    private void loadGigsByArticle(Article article) {
        gigsContainer.getChildren().clear();

        if (article == null) {
            Label noArticleLabel = new Label("Aucun article sélectionné.");
            noArticleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            gigsContainer.getChildren().add(noArticleLabel);
            return;
        }

        List<String> keywords = extractKeywords(article);
        List<GigModel> gigs = new ArrayList<>();

        try {
            gigs = findGigsByKeywords(keywords);
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
    }

    private VBox createGigCard(GigModel gig) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
                    -fx-border-color: #ddd;
                    -fx-border-radius: 10;
                    -fx-background-radius: 10;
                    -fx-background-color: white;
                    -fx-padding: 15;
                    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 10, 0, 0, 4);
                    -fx-max-width: 250px;
                    -fx-min-width: 250px;
                """);

        // Image du Gig
        ImageView gigImage;
        try {
            String imagePath = gig.getImage(); // Chemin de l'image
            if (imagePath != null && !imagePath.isEmpty()) {
                Image image = new Image(new File(imagePath).toURI().toString(), 200, 120, true, true);
                gigImage = new ImageView(image);
            } else {
                // Si l'image est introuvable, utiliser une image par défaut
                gigImage = new ImageView(
                        new Image(getClass().getResource("/com/khademni/default-image.png").toExternalForm()));
            }
        } catch (Exception e) {
            // En cas d'erreur, utiliser une image par défaut
            gigImage = new ImageView(
                    new Image(getClass().getResource("/com/khademni/default-image.png").toExternalForm()));
        }
        gigImage.setFitWidth(200);
        gigImage.setFitHeight(120);
        gigImage.setPreserveRatio(true);
        gigImage.setStyle("-fx-border-radius: 10; -fx-background-radius: 10;");

        // Titre du Gig
        Label gigTitle = new Label(gig.getTitle());
        gigTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        gigTitle.setWrapText(true);

        // Description du Gig
        Label gigDescription = new Label(gig.getDescription());
        gigDescription.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        gigDescription.setWrapText(true);

        // Prix du Gig
        Label gigPrice = new Label(String.format("Prix : %.2f TND", gig.getPrice()));
        gigPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");

        // Ajouter les éléments à la carte
        card.getChildren().addAll(gigImage, gigTitle, gigDescription, gigPrice);

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
                        rs.getString("status"));

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
        String content = article.getTitle() + " " + article.getContent();
        String[] words = content.split("\\W+"); // Sépare les mots par des caractères non alphabétiques
        List<String> keywords = new ArrayList<>();

        for (String word : words) {
            if (word.length() > 3) { // Inclure uniquement les mots de plus de 3 caractères
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    private UserModel getCurrentUser() {
        // Replace with the actual logic to retrieve the logged-in user
        return SessionManager.getCurrentUser(); // Example: Using a SessionManager class
    }

    private void chargerCommentaires(Article article, VBox commentairesBox) {
    commentairesBox.getChildren().clear();

    try {
        // Récupérer les commentaires pour l'article donné
        var commentaires = commentaireService.findByArticleId(article.getId());
        UserModel currentUser = SessionManager.getCurrentUser();

        for (Commentaire c : commentaires) {
            // Afficher l'auteur et la date
            Label authorLabel = new Label(c.getUser().getFirstName() + " " + c.getUser().getLastName());
            authorLabel.getStyleClass().add("author");

            Label timeLabel = new Label(formatTimeAgo(c.getCreatedAt()));
            timeLabel.getStyleClass().add("time");

            HBox authorTimeBox = new HBox(10, authorLabel, timeLabel);
            authorTimeBox.setAlignment(Pos.CENTER_LEFT);

            // Contenu du commentaire
            Label contentLabel = new Label(c.getContent());
            contentLabel.getStyleClass().add("content");

            VBox commentContentBox = new VBox(5, authorTimeBox, contentLabel);
            commentContentBox.getStyleClass().add("comment-box");

            // Boutons pour modifier et supprimer (si l'utilisateur est le propriétaire)
            if (currentUser != null && c.getUser().getId() == currentUser.getId()) {
                Button editBtn = new Button("Edit");
                editBtn.getStyleClass().add("edit-btn");

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("delete-btn");

                editBtn.setOnAction(ev -> editComment(c));
                deleteBtn.setOnAction(ev -> deleteComment(c, commentairesBox));

                HBox buttonBox = new HBox(10, editBtn, deleteBtn);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                commentContentBox.getChildren().add(buttonBox);
            }

            commentairesBox.getChildren().add(commentContentBox);
        }

        // ===== Champ pour ajouter un nouveau commentaire =====
        TextArea newCommentArea = new TextArea();
        newCommentArea.setPromptText("Write a comment...");
        newCommentArea.setWrapText(true);
        newCommentArea.setPrefRowCount(2);
        newCommentArea.setStyle("-fx-border-color: #ce93d8; -fx-border-radius: 5; -fx-background-radius: 5;");

        Button addCommentBtn = new Button("Add Comment");
        addCommentBtn.setStyle(
                "-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 5; -fx-background-radius: 5;");

        addCommentBtn.setOnAction(ev -> {
            String content = newCommentArea.getText().trim();
            if (content.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Erreur", "Le commentaire ne peut pas être vide.");
                return;
            }

            System.out.println("Debug: Checking if comment is acceptable...");
            boolean isAcceptable = ContentModeration.isCommentAcceptable(content);

            if (!isAcceptable) {
                System.out.println("Debug: Comment is not acceptable.");
                showAlert(Alert.AlertType.ERROR, "Commentaire rejeté", "Votre commentaire contient des propos inappropriés.");
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
                        currentUser);

                commentaireService.create(newComment); // Ajouter le nouveau commentaire dans la base de données
                newCommentArea.clear();
                chargerCommentaires(article, commentairesBox); // Recharger les commentaires
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commentaire ajouté avec succès !");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le commentaire.");
            }
        });

        HBox inputBox = new HBox(10, newCommentArea, addCommentBtn);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setPadding(new Insets(10, 0, 0, 0));

        commentairesBox.getChildren().add(inputBox);

    } catch (SQLException e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commentaires.");
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
                if (node instanceof VBox) { // Ensure the node is a VBox
                    VBox commentBox = (VBox) node;
                    Label contentLabel = (Label) commentBox.getChildren().get(1); // Get the content label
                    return contentLabel.getText().equals(commentaire.getContent());
                }
                return false;
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

        if (minutes < 1)
            return "just now";
        if (minutes < 60)
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        if (hours < 24)
            return hours + (hours == 1 ? " hour ago" : " hours ago");
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

            // Créer un objet Favori
            Favori favori = new Favori(currentUser.getId(), article.getId());

            // Vérifier si le favori existe
            if (favoriService.isFavori(favori)) {
                // Supprimer le favori
                Favori existingFavori = favoriService.findByUserAndArticle(currentUser.getId(), article.getId());
                if (existingFavori != null) {
                    favoriService.delete(existingFavori.getId());
                }
            } else {
                // Ajouter le favori
                favoriService.create(favori);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Erreur", "Une erreur s'est produite.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Erreur", "Une erreur s'est produite.");
        }
    }

}