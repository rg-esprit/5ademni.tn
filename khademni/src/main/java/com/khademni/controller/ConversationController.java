package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ConversationModel;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConversationController {

    @FXML private ListView<HBox> listConversations;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private Label errorLabel;
    @FXML private Button btnArchiver;
    @FXML private Button btnActiver;
    @FXML private ComboBox<String> cmbStatut;

    private Connection connection;
    private ObservableList<Map<String, Object>> conversationsList = FXCollections.observableArrayList();
    private int currentUserId;

    public ConversationController() {
        try {
            connection = MyDataBase.getConnection();
            currentUserId = App.getCurrentUser().getId();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        if (connection == null) {
            showError("Connexion DB échouée !");
            return;
        }

        setupFilters();
        setupSearch();
        setupListClickHandler();
        loadConversations();
    }

    private void setupFilters() {
        // Initialisation des filtres dans le code Java (plus fiable que FXML)
        cmbFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "Non lues", "Avec offres", "Avec contrats"
        ));
        cmbFilter.setValue("Toutes");

        cmbFilter.setOnAction(e -> loadConversations());

        // Initialisation du statut
        cmbStatut.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        btnArchiver.setDisable(true);
        btnActiver.setDisable(true);
        cmbStatut.setDisable(true);
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            loadConversationsWithSearch(newVal);
        });

        // Bouton pour effacer la recherche (optionnel)
        TextField searchField = txtSearch;
        searchField.setOnMouseClicked(e -> {
            if (!searchField.getText().isEmpty()) {
                searchField.clear();
            }
        });
    }

    private void setupListClickHandler() {
        listConversations.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleOpenChat();
            }
        });
    }

    private void loadConversations() {
        loadConversationsWithSearch(txtSearch.getText().trim());
    }

    private void loadConversationsWithSearch(String searchTerm) {
        conversationsList.clear();
        try {
            String sql = "SELECT c.*, " +
                    "u1.first_name as client_first, u1.last_name as client_last, u1.email as client_email, " +
                    "u2.first_name as freelance_first, u2.last_name as freelance_last, u2.email as freelance_email, " +
                    "(SELECT COUNT(*) FROM message WHERE conversation_id = c.id) as msg_count, " +
                    "(SELECT contenu FROM message WHERE conversation_id = c.id ORDER BY date_envoie DESC LIMIT 1) as last_msg, " +
                    "(SELECT date_envoie FROM message WHERE conversation_id = c.id ORDER BY date_envoie DESC LIMIT 1) as last_msg_time " +
                    "FROM conversation c " +
                    "JOIN users u1 ON c.client_id = u1.id " +
                    "JOIN users u2 ON c.freelance_id = u2.id " +
                    "WHERE (c.client_id = ? OR c.freelance_id = ?) ";

            // Ajouter les conditions de recherche si un terme est fourni
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql += " AND ( " +
                        "c.titre LIKE ? OR " +
                        "u1.first_name LIKE ? OR u1.last_name LIKE ? OR " +
                        "u2.first_name LIKE ? OR u2.last_name LIKE ? OR " +
                        "CONCAT(u1.first_name, ' ', u1.last_name) LIKE ? OR " +
                        "CONCAT(u2.first_name, ' ', u2.last_name) LIKE ? " +
                        ") ";
            }

            // Ajouter le filtre
            String filter = cmbFilter.getValue();
            if ("Non lues".equals(filter)) {
                sql += " AND ( " +
                        "(c.client_id = ? AND c.non_lus_client > 0) OR " +
                        "(c.freelance_id = ? AND c.non_lus_freelance > 0) " +
                        ") ";
            } else if ("Avec offres".equals(filter)) {
                sql += " AND c.id IN (SELECT DISTINCT conversation_id FROM message WHERE contenu LIKE '%offre%') ";
            } else if ("Avec contrats".equals(filter)) {
                sql += " AND c.id IN (SELECT DISTINCT conversation_id FROM contrat) ";
            }

            sql += " ORDER BY last_msg_time DESC";

            PreparedStatement ps = connection.prepareStatement(sql);
            int paramIndex = 1;
            ps.setInt(paramIndex++, currentUserId);
            ps.setInt(paramIndex++, currentUserId);

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchPattern = "%" + searchTerm.trim() + "%";
                for (int i = 0; i < 7; i++) {
                    ps.setString(paramIndex++, searchPattern);
                }
            }

            if ("Non lues".equals(filter)) {
                ps.setInt(paramIndex++, currentUserId);
                ps.setInt(paramIndex++, currentUserId);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> conv = new HashMap<>();
                conv.put("id", rs.getInt("id"));
                conv.put("client_id", rs.getInt("client_id"));
                conv.put("freelance_id", rs.getInt("freelance_id"));
                conv.put("client_name", rs.getString("client_first") + " " + rs.getString("client_last"));
                conv.put("freelance_name", rs.getString("freelance_first") + " " + rs.getString("freelance_last"));
                conv.put("statut", rs.getString("statut"));
                conv.put("titre", rs.getString("titre"));
                conv.put("msg_count", rs.getInt("msg_count"));
                conv.put("last_msg", rs.getString("last_msg"));
                conv.put("last_msg_time", rs.getTimestamp("last_msg_time"));
                conv.put("non_lus_client", rs.getInt("non_lus_client"));
                conv.put("non_lus_freelance", rs.getInt("non_lus_freelance"));
                conversationsList.add(conv);
            }

            updateConversationsList(searchTerm);

        } catch (SQLException e) {
            showError("Erreur chargement conversations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateConversationsList(String searchTerm) {
        listConversations.getItems().clear();

        for (Map<String, Object> conv : conversationsList) {
            HBox item = createConversationItem(conv, searchTerm);
            listConversations.getItems().add(item);
        }

        if (conversationsList.isEmpty()) {
            Label emptyLabel = new Label("Aucune conversation trouvée");
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 20; -fx-font-size: 14;");
            HBox emptyBox = new HBox(emptyLabel);
            emptyBox.setAlignment(Pos.CENTER);
            listConversations.getItems().add(emptyBox);
        }
    }

    private HBox createConversationItem(Map<String, Object> conv, String searchTerm) {
        HBox item = new HBox(15);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: white; -fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
        item.setAlignment(Pos.CENTER_LEFT);

        // Avatar avec initiales et couleur selon le statut
        String otherName = getOtherParticipantName(conv);
        String initiales = getInitiales(otherName);
        Label avatar = new Label(initiales);

        String avatarColor = "#2563eb"; // Bleu par défaut
        if (conv.get("statut") != null && "INACTIVE".equals(conv.get("statut"))) {
            avatarColor = "#9ca3af"; // Gris pour inactif
        }

        avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-min-width: 45; -fx-min-height: 45; -fx-max-width: 45; -fx-max-height: 45; " +
                "-fx-background-radius: 25; -fx-alignment: center; -fx-font-size: 16;");

        // Informations
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Nom avec surlignage si recherche
        String displayName = otherName;
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");

        // Titre du service si présent
        if (conv.get("titre") != null) {
            String titre = conv.get("titre").toString();
            Label serviceLabel = new Label("📌 " + titre);
            serviceLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12; -fx-background-color: #e0f2fe; -fx-padding: 2 8; -fx-background-radius: 12;");
            titleBox.getChildren().addAll(nameLabel, serviceLabel);
        } else {
            titleBox.getChildren().add(nameLabel);
        }

        // Dernier message
        String lastMsg = conv.get("last_msg") != null ? conv.get("last_msg").toString() : "Aucun message";
        if (lastMsg.length() > 50) lastMsg = lastMsg.substring(0, 47) + "...";
        Label lastMsgLabel = new Label(lastMsg);
        lastMsgLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");

        infoBox.getChildren().addAll(titleBox, lastMsgLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Date et compteur de non lus
        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        if (conv.get("last_msg_time") != null) {
            Timestamp ts = (Timestamp) conv.get("last_msg_time");
            String time = formatTimeAgo(ts.toLocalDateTime());
            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");
            rightBox.getChildren().add(timeLabel);
        }

        // Badge de non lus
        int nonLus = 0;
        int clientId = (int) conv.get("client_id");
        if (clientId == currentUserId) {
            nonLus = (int) conv.getOrDefault("non_lus_client", 0);
        } else {
            nonLus = (int) conv.getOrDefault("non_lus_freelance", 0);
        }

        if (nonLus > 0) {
            Label unreadLabel = new Label(String.valueOf(nonLus));
            unreadLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-min-width: 22; -fx-min-height: 22; -fx-max-width: 22; -fx-max-height: 22; " +
                    "-fx-background-radius: 11; -fx-alignment: center; -fx-font-size: 11;");
            rightBox.getChildren().add(unreadLabel);
        }

        item.getChildren().addAll(avatar, infoBox, spacer, rightBox);
        item.setUserData(conv.get("id"));

        // Animation au survol
        item.setOnMouseEntered(e ->
                item.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0; -fx-cursor: hand;")
        );
        item.setOnMouseExited(e ->
                item.setStyle("-fx-background-color: white; -fx-border-color: #f3f4f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;")
        );

        return item;
    }

    private String getOtherParticipantName(Map<String, Object> conv) {
        int clientId = (int) conv.get("client_id");
        if (clientId == currentUserId) {
            return conv.get("freelance_name").toString();
        } else {
            return conv.get("client_name").toString();
        }
    }

    private String getInitiales(String fullName) {
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return fullName.substring(0, 1).toUpperCase();
    }

    private String formatTimeAgo(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();

        if (time.toLocalDate().equals(now.toLocalDate())) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (time.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
            return "Hier";
        } else if (time.isAfter(now.minusWeeks(1))) {
            return time.format(DateTimeFormatter.ofPattern("EEE"));
        } else {
            return time.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        }
    }

    @FXML
    private void handleOpenChat() {
        HBox selected = listConversations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUserData() == null) {
            showError("Veuillez sélectionner une conversation");
            return;
        }

        int convId = (int) selected.getUserData();

        try {
            App.setPendingConversationId(convId);
            App.setRoot("Message");
        } catch (IOException e) {
            showError("Erreur ouverture chat");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        // Cette méthode sera appelée depuis le FXML
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle conversation");
        dialog.setHeaderText("Créer une nouvelle conversation");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        ComboBox<String> cmbUser1 = new ComboBox<>();
        cmbUser1.setPromptText("Sélectionner un utilisateur");
        ComboBox<String> cmbUser2 = new ComboBox<>();
        cmbUser2.setPromptText("Sélectionner un autre utilisateur");

        // Charger les utilisateurs
        loadUsersForCombo(cmbUser1, cmbUser2);

        content.getChildren().addAll(
                new Label("Premier participant:"),
                cmbUser1,
                new Label("Second participant:"),
                cmbUser2
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Créer la conversation
                showSuccess("Fonctionnalité à implémenter");
            }
        });
    }

    private void loadUsersForCombo(ComboBox<String> combo1, ComboBox<String> combo2) {
        try {
            String sql = "SELECT CONCAT(first_name, ' ', last_name, ' (', email, ')') as display FROM users";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String display = rs.getString("display");
                combo1.getItems().add(display);
                combo2.getItems().add(display);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleArchiver() {
        updateStatut("INACTIVE");
    }

    @FXML
    private void handleActiver() {
        updateStatut("ACTIVE");
    }

    @FXML
    private void handleChangerStatut() {
        HBox selected = listConversations.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUserData() != null && cmbStatut.getValue() != null) {
            updateStatut(cmbStatut.getValue());
        }
    }

    private void updateStatut(String nouveauStatut) {
        HBox selected = listConversations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUserData() == null) {
            showError("Sélectionnez une conversation");
            return;
        }

        int convId = (int) selected.getUserData();

        try {
            String req = "UPDATE conversation SET statut = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, nouveauStatut);
            ps.setInt(2, convId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                showSuccess("Statut modifié : " + nouveauStatut);
                loadConversations();
            }

        } catch (SQLException e) {
            showError("Erreur mise à jour: " + e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        HBox selected = listConversations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUserData() == null) {
            showError("Sélectionnez une conversation");
            return;
        }

        int convId = (int) selected.getUserData();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(listConversations.getScene().getWindow());
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la conversation ?");
        alert.setContentText("Tous les messages seront définitivement supprimés.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                connection.setAutoCommit(false);

                PreparedStatement ps1 = connection.prepareStatement("DELETE FROM message WHERE conversation_id = ?");
                ps1.setInt(1, convId);
                ps1.executeUpdate();

                PreparedStatement ps2 = connection.prepareStatement("DELETE FROM conversation WHERE id = ?");
                ps2.setInt(1, convId);
                ps2.executeUpdate();

                connection.commit();
                loadConversations();
                showSuccess("Conversation supprimée");

            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ex) {}
                showError("Erreur suppression");
                e.printStackTrace();
            } finally {
                try { connection.setAutoCommit(true); } catch (SQLException e) {}
            }
        }
    }

    @FXML
    private void handleManageMembers() {
        HBox selected = listConversations.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUserData() == null) {
            showError("Sélectionnez une conversation");
            return;
        }

        int convId = (int) selected.getUserData();
        showMembersDialog(convId);
    }

    private void showMembersDialog(int convId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("👥 Gérer les membres");
        dialog.setHeaderText("Conversation #" + convId);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        // Liste des membres
        ListView<HBox> membersList = new ListView<>();
        membersList.setPrefHeight(200);

        loadMembersList(membersList, convId);

        // Formulaire d'ajout
        HBox addBox = new HBox(10);
        ComboBox<String> cmbUsers = new ComboBox<>();
        cmbUsers.setPromptText("Choisir un utilisateur");
        cmbUsers.setPrefWidth(250);
        loadAvailableUsers(cmbUsers, convId);

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("MEMBER", "ADMIN");
        cmbRole.setValue("MEMBER");
        cmbRole.setPrefWidth(100);

        Button btnAdd = new Button("Ajouter");
        btnAdd.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;");

        addBox.getChildren().addAll(cmbUsers, cmbRole, btnAdd);

        content.getChildren().addAll(new Label("Membres actuels :"), membersList,
                new Separator(), new Label("Ajouter un membre :"), addBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        btnAdd.setOnAction(e -> {
            String selectedUser = cmbUsers.getValue();
            String role = cmbRole.getValue();
            if (selectedUser != null && !selectedUser.isEmpty()) {
                addMember(convId, selectedUser, role);
                loadMembersList(membersList, convId);
                loadAvailableUsers(cmbUsers, convId);
            }
        });

        dialog.showAndWait();
    }

    private void loadMembersList(ListView<HBox> listView, int convId) {
        listView.getItems().clear();
        try {
            String sql = "SELECT cm.*, u.first_name, u.last_name, u.email " +
                    "FROM conversation_members cm " +
                    "JOIN users u ON cm.user_id = u.id " +
                    "WHERE cm.conversation_id = ? AND cm.status = 'ACTIVE'";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                HBox item = new HBox(10);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setPadding(new Insets(5));

                String firstName = rs.getString("first_name");
                Label avatar = new Label(firstName.substring(0, 1).toUpperCase());
                avatar.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15; -fx-alignment: center; -fx-font-weight: bold;");

                VBox info = new VBox(2);
                Label nameLabel = new Label(firstName + " " + rs.getString("last_name"));
                nameLabel.setStyle("-fx-font-weight: bold;");
                Label detailsLabel = new Label(rs.getString("email") + " · " + rs.getString("role"));
                detailsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
                info.getChildren().addAll(nameLabel, detailsLabel);

                item.getChildren().addAll(avatar, info);
                listView.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAvailableUsers(ComboBox<String> combo, int convId) {
        combo.getItems().clear();
        try {
            String sql = "SELECT CONCAT(first_name, ' ', last_name, ' (', email, ')') as display " +
                    "FROM users WHERE id NOT IN " +
                    "(SELECT user_id FROM conversation_members WHERE conversation_id = ? AND status = 'ACTIVE')";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                combo.getItems().add(rs.getString("display"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addMember(int convId, String selectedUser, String role) {
        try {
            String email = selectedUser.replaceAll(".*\\((.*)\\).*", "$1");
            String sql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String insert = "INSERT INTO conversation_members (conversation_id, user_id, role, invited_by) VALUES (?, ?, ?, ?)";
                PreparedStatement psInsert = connection.prepareStatement(insert);
                psInsert.setInt(1, convId);
                psInsert.setInt(2, userId);
                psInsert.setString(3, role);
                psInsert.setInt(4, currentUserId);
                psInsert.executeUpdate();
                showSuccess("Membre ajouté");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                showError("Cet utilisateur est déjà membre");
            } else {
                showError("Erreur ajout membre");
                e.printStackTrace();
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void showSuccess(String message) {
        errorLabel.setText("✅ " + message);
        errorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}