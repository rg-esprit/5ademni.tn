package com.khademni.controller;

import com.khademni.model.Article;
import com.khademni.model.GigModel;
import com.khademni.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GigDisplayController {

    @FXML
    private TilePane gigsContainer;

    @FXML
    private ScrollPane gigsScrollPane;

    private Article article;

    public void setArticle(Article article) {
        this.article = article;
        loadGigsByArticle();
    }

    private void loadGigsByArticle() {
    gigsContainer.getChildren().clear();

    if (article == null) {
        Label noArticleLabel = new Label("Aucun article sélectionné.");
        noArticleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
        gigsContainer.getChildren().add(noArticleLabel);
        return;
    }
//extractKeywords 
    List<String> keywords = extractKeywords(article);
    List<GigModel> gigs = fetchGigsByKeywords(keywords);

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
   @FXML
    private void goBackToArticles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Article/ArticlesPublications.fxml"));
            Parent articlesPage = loader.load();

            // Get the current stage and set the new scene
            Stage stage = (Stage) gigsContainer.getScene().getWindow();
            stage.setScene(new Scene(articlesPage));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
private List<GigModel> fetchGigsByKeywords(List<String> keywords) {
    List<GigModel> gigs = new ArrayList<>();
    String query = """
        SELECT g.id, g.title, g.description, g.price, g.delivery_time, g.image, g.status
        FROM gig g
    """;

    try (Connection conn = MyDataBase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {

        ResultSet rs = stmt.executeQuery();
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

            // Vérifiez si au moins deux mots correspondent
            if (hasAtLeastTwoMatchingWords(keywords, gig)) {
                gigs.add(gig);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return gigs;
}
private boolean hasAtLeastTwoMatchingWords(List<String> keywords, GigModel gig) {
    // Combine le titre et la description du Gig
    String gigContent = (gig.getTitle() + " " + gig.getDescription()).toLowerCase();

    int matchCount = 0;

    // Comptez les mots correspondants
    for (String keyword : keywords) {
        if (gigContent.contains(keyword.toLowerCase())) {
            matchCount++;
        }
        if (matchCount >= 2) {
            return true; // Au moins deux mots correspondent
        }
    }

    return false; // Moins de deux mots correspondent
}

private List<String> extractKeywords(Article article) {
    String content = article.getTitle() + " " + article.getContent();
    String[] words = content.split("\\W+");
    List<String> keywords = new ArrayList<>();
    for (String word : words) {
        if (word.length() > 3) { // Only include words longer than 3 characters
            keywords.add(word.toLowerCase());
        }
    }
    return keywords;
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
        // Convertir le chemin de l'image en URL valide
    ImageView gigImage;
    try {
        String imagePath = gig.getImage(); // Chemin de l'image
        Image image = new Image(new File(imagePath).toURI().toString());
        gigImage = new ImageView(image);
    } catch (Exception e) {
        // Si l'image est introuvable, utiliser une image par défaut
        gigImage = new ImageView(new Image(getClass().getResource("/com/khademni/default-image.png").toExternalForm()));
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
}