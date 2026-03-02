package com.khademni.controller;

import com.khademni.model.Favori;
import com.khademni.service.FavoriController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FavoriControllerGUI {

    @FXML
    private ListView<Favori> listFavoris;

    @FXML
    public void initialize() {
        loadFavoris();

        listFavoris.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Favori f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setGraphic(null);
                    return;
                }

                Label title = new Label(f.getArticle().getTitle());
                Label date = new Label(
                        "Ajouté le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                VBox box = new VBox(5, title, date);
                box.setStyle(
                        "-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                setGraphic(box);
            }
        });
    }

    private void loadFavoris() {
        try {
            listFavoris.getItems().setAll(new FavoriController().findAll());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
