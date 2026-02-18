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

        VBox card = new VBox();
        card.setPrefWidth(800);
        card.setStyle("""
            -fx-background-color: white;
            -fx-padding: 15;
            -fx-background-radius: 15;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);
        """);

        HBox mainContainer = new HBox(20);
        mainContainer.setAlignment(Pos.CENTER_LEFT);

        // IMAGE À GAUCHE
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        if (article.getImagePath() != null) {
            imageView.setImage(new Image("file:" + article.getImagePath()));
        }

        // CENTRE (Titre + Description + Date)
        VBox contentBox = new VBox(8);

        Label title = new Label(article.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label description = new Label(article.getContent());
        description.setWrapText(true);
        description.setMaxWidth(450);

        Label dateLabel = new Label(
            createdAt != null
                    ? "Ajouté le : " + createdAt.toLocalDate()
                    : "Date inconnue"
        );

        dateLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        contentBox.getChildren().addAll(title, description, dateLabel);
        // Vérifier si l'article est favori et changer la couleur du titre
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: red;");

        // BOUTON À DROITE
        Button removeBtn = new Button("Retirer");
        removeBtn.setStyle("""
            -fx-background-color: #ff4d4d;
            -fx-text-fill: white;
            -fx-background-radius: 20;
            -fx-padding: 8 20;
        """);

        removeBtn.setOnAction(e -> {
            try {
                favoriService.delete(favoriId);
                loadFavoris();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        mainContainer.getChildren().addAll(imageView, contentBox, spacer, removeBtn);

        card.getChildren().add(mainContainer);

        return card;
    }
}