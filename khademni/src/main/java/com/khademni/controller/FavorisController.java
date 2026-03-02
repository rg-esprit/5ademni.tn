package com.khademni.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import com.khademni.App;
import com.khademni.model.Article;
import com.khademni.model.Favori;
import com.khademni.service.ArticleController;
import com.khademni.service.FavoriController;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import java.time.LocalDateTime;
import com.khademni.model.UserModel;
import java.sql.SQLException;
import java.util.List;

public class FavorisController {

    @FXML
    private TilePane favorisContainer;

    private final ArticleController articleService = new ArticleController();
    private final FavoriController favoriService = new FavoriController();
    private UserModel currentUser;

    @FXML
    public void initialize() {
        currentUser = new UserModel();
        currentUser.setId(1); // Exemple : ID de l'utilisateur connecté
        currentUser.setFirstName("UtilisateurTest");

        loadFavoris();
    }

   private void loadFavoris() {
    try {
        favorisContainer.getChildren().clear();

        // Récupérer l'utilisateur connecté
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) {
            System.out.println("Aucun utilisateur connecté.");
            return;
        }

        // Filtrer les favoris par utilisateur connecté
        List<Favori> favoris = favoriService.findByUserId((long) currentUser.getId());

        for (Favori favori : favoris) {
            // Charger l'article depuis la base via son id
            Article article = articleService.findById(favori.getArticle().getId());

            if (article != null) {
                VBox card = createFavoriCard(article, favori.getId(), favori.getCreatedAt());
                favorisContainer.getChildren().add(card);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

   private VBox createFavoriCard(Article article, Long favoriId, LocalDateTime createdAt) {
    // ===== CARTE PRINCIPALE =====
    VBox card = new VBox(10);
    card.setAlignment(Pos.CENTER);
    card.setStyle("""
        -fx-background-color: white;
        -fx-padding: 20;
        -fx-background-radius: 16;
        -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 20, 0, 0, 5);
    """);
    card.setPrefWidth(600); // Ajustez la largeur pour centrer les cartes

    // ================= IMAGE =================
    ImageView imageView = new ImageView();
    imageView.setFitWidth(100);
    imageView.setFitHeight(100);
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

    // ================= CONTENU =================
    VBox contentBox = new VBox(8);
    contentBox.setAlignment(Pos.CENTER_LEFT);

    Label title = new Label(article.getTitle());
    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6c0df2;");

    Label description = new Label(article.getContent());
    description.setWrapText(true);
    description.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

    Label date = new Label("Ajouté le : " + article.getCreatedAt());
    date.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

    contentBox.getChildren().addAll(title, description, date);

    // ================= ACTIONS =================
    Button removeBtn = new Button("Retirer");
    removeBtn.setStyle("""
        -fx-background-color: #f87171;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.3), 10, 0, 0, 4);
    """);

    removeBtn.setOnMouseEntered(e -> removeBtn.setStyle("""
        -fx-background-color: #ef4444;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.5), 12, 0, 0, 6);
    """));

    removeBtn.setOnMouseExited(e -> removeBtn.setStyle("""
        -fx-background-color: #f87171;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-background-radius: 20;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.3), 10, 0, 0, 4);
    """));

    removeBtn.setOnAction(e -> {
        try {
            favoriService.delete(favoriId);
            loadFavoris();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    });

    HBox actionsBox = new HBox(removeBtn);
    actionsBox.setAlignment(Pos.CENTER_RIGHT);

    // ================= LAYOUT PRINCIPAL =================
    HBox mainRow = new HBox(20, imageView, contentBox, actionsBox);
    mainRow.setAlignment(Pos.CENTER_LEFT);

    card.getChildren().add(mainRow);

    return card;
}
}