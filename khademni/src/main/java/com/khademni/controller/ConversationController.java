package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ConversationModel;
import com.khademni.utils.MyDataBase;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.stage.Stage;

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
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Button btnMigrate;

    private Connection connection;
    private ObservableList<Map<String, Object>> conversationsList = FXCollections.observableArrayList();
    private int currentUserId;
    private boolean migrationDone = false;

    public ConversationController() {
        try {
            connection = MyDataBase.getConnection();
            currentUserId = App.getCurrentUser().getId();
            createConversationMembersTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createConversationMembersTable() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS conversation_members (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "conversation_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "role ENUM('MEMBER', 'ADMIN', 'OWNER') DEFAULT 'MEMBER', " +
                    "status ENUM('ACTIVE', 'INACTIVE', 'LEFT') DEFAULT 'ACTIVE', " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "invited_by INT, " +
                    "FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL, " +
                    "UNIQUE KEY unique_conversation_user (conversation_id, user_id)" +
                    ")";
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            System.out.println("✅ Table conversation_members vérifiée/créée");

            try {
                DatabaseMetaData md = connection.getMetaData();
                ResultSet rs = md.getColumns(null, null, "conversation", "created_by");
                if (!rs.next()) {
                    String alterSql = "ALTER TABLE conversation ADD COLUMN created_by INT, " +
                            "ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
                    stmt.execute(alterSql);
                    System.out.println("✅ Colonnes ajoutées à la table conversation");
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Erreur lors de l'ajout des colonnes: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur création table: " + e.getMessage());
        }
    }

    private void autoMigrateExistingConversations() {
        if (migrationDone) return;

        try {
            String sql1 = "INSERT IGNORE INTO conversation_members (conversation_id, user_id, role, status, joined_at, invited_by) " +
                    "SELECT c.id, c.client_id, 'OWNER', 'ACTIVE', COALESCE(c.created_at, NOW()), c.client_id " +
                    "FROM conversation c " +
                    "WHERE NOT EXISTS (SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = c.id AND cm.user_id = c.client_id)";
            Statement st1 = connection.createStatement();
            int clientRows = st1.executeUpdate(sql1);

            String sql2 = "INSERT IGNORE INTO conversation_members (conversation_id, user_id, role, status, joined_at, invited_by) " +
                    "SELECT c.id, c.freelance_id, 'OWNER', 'ACTIVE', COALESCE(c.created_at, NOW()), c.freelance_id " +
                    "FROM conversation c " +
                    "WHERE NOT EXISTS (SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = c.id AND cm.user_id = c.freelance_id)";
            Statement st2 = connection.createStatement();
            int freelanceRows = st2.executeUpdate(sql2);

            if (clientRows > 0 || freelanceRows > 0) {
                System.out.println("✅ Migration automatique des membres: " + clientRows + " clients, " + freelanceRows + " freelances");
            }

            migrationDone = true;

        } catch (SQLException e) {
            System.err.println("❌ Erreur migration automatique: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        if (connection == null) {
            showError("Connexion DB échouée !");
            return;
        }

        autoMigrateExistingConversations();

        setupFilters();
        setupSearch();
        setupListClickHandler();
        loadConversations();

        if (btnMigrate != null) {
            btnMigrate.setOnAction(e -> handleManualMigration());
            btnMigrate.setVisible(false);
        }
    }

    private void setupFilters() {
        cmbFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "Non lues", "Avec offres", "Avec contrats"
        ));
        cmbFilter.setValue("Toutes");
        cmbFilter.setOnAction(e -> loadConversations());

        cmbStatut.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
    }

    private void setupSearch() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            loadConversationsWithSearch(newVal);
        });

        txtSearch.setOnMouseClicked(e -> {
            if (!txtSearch.getText().isEmpty()) {
                txtSearch.clear();
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

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql += " AND ( " +
                        "c.titre LIKE ? OR " +
                        "u1.first_name LIKE ? OR u1.last_name LIKE ? OR " +
                        "u2.first_name LIKE ? OR u2.last_name LIKE ? OR " +
                        "CONCAT(u1.first_name, ' ', u1.last_name) LIKE ? OR " +
                        "CONCAT(u2.first_name, ' ', u2.last_name) LIKE ? " +
                        ") ";
            }

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

        String otherName = getOtherParticipantName(conv);
        String initiales = getInitiales(otherName);
        Label avatar = new Label(initiales);

        String avatarColor = "#8b5cf6";
        if (conv.get("statut") != null && "INACTIVE".equals(conv.get("statut"))) {
            avatarColor = "#9ca3af";
        }

        avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-min-width: 45; -fx-min-height: 45; -fx-max-width: 45; -fx-max-height: 45; " +
                "-fx-background-radius: 25; -fx-alignment: center; -fx-font-size: 16;");

        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        String displayName = otherName;
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1f2937;");

        if (conv.get("titre") != null) {
            String titre = conv.get("titre").toString();
            Label serviceLabel = new Label("📌 " + titre);
            serviceLabel.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 12; -fx-background-color: #f3e8ff; -fx-padding: 2 8; -fx-background-radius: 12;");
            titleBox.getChildren().addAll(nameLabel, serviceLabel);
        } else {
            titleBox.getChildren().add(nameLabel);
        }

        String lastMsg = conv.get("last_msg") != null ? conv.get("last_msg").toString() : "Aucun message";
        if (lastMsg.length() > 50) lastMsg = lastMsg.substring(0, 47) + "...";
        Label lastMsgLabel = new Label(lastMsg);
        lastMsgLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");

        infoBox.getChildren().addAll(titleBox, lastMsgLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        if (conv.get("last_msg_time") != null) {
            Timestamp ts = (Timestamp) conv.get("last_msg_time");
            String time = formatTimeAgo(ts.toLocalDateTime());
            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");
            rightBox.getChildren().add(timeLabel);
        }

        int nonLus = 0;
        int clientId = (int) conv.get("client_id");
        if (clientId == currentUserId) {
            nonLus = (int) conv.getOrDefault("non_lus_client", 0);
        } else {
            nonLus = (int) conv.getOrDefault("non_lus_freelance", 0);
        }

        if (nonLus > 0) {
            Label unreadLabel = new Label(String.valueOf(nonLus));
            unreadLabel.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-min-width: 22; -fx-min-height: 22; -fx-max-width: 22; -fx-max-height: 22; " +
                    "-fx-background-radius: 11; -fx-alignment: center; -fx-font-size: 11;");
            rightBox.getChildren().add(unreadLabel);
        }

        item.getChildren().addAll(avatar, infoBox, spacer, rightBox);
        item.setUserData(conv.get("id"));

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

        Map<String, Object> conv = getConversationById(convId);
        int clientId = (int) conv.get("client_id");
        int freelanceId = (int) conv.get("freelance_id");

        System.out.println("🔍 Ouverture conversation #" + convId + " - Client: " + clientId + ", Freelance: " + freelanceId);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Message.fxml"));
            Parent root = loader.load();

            MessageController messageController = loader.getController();
            messageController.setConversationId(convId);
            messageController.setConversationInfo(clientId, freelanceId);

            Stage stage = (Stage) listConversations.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Conversation #" + convId);

        } catch (IOException e) {
            showError("Erreur ouverture chat");
            e.printStackTrace();
        }
    }

    private Map<String, Object> getConversationById(int convId) {
        Map<String, Object> conv = new HashMap<>();
        try {
            String sql = "SELECT * FROM conversation WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                conv.put("client_id", rs.getInt("client_id"));
                conv.put("freelance_id", rs.getInt("freelance_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conv;
    }

    @FXML
    private void handleAjouter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle conversation");
        dialog.setHeaderText("Créer une nouvelle conversation");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        ComboBox<String> cmbUser1 = new ComboBox<>();
        cmbUser1.setPromptText("Sélectionner un utilisateur");
        ComboBox<String> cmbUser2 = new ComboBox<>();
        cmbUser2.setPromptText("Sélectionner un autre utilisateur");

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

                PreparedStatement ps2 = connection.prepareStatement("DELETE FROM conversation_members WHERE conversation_id = ?");
                ps2.setInt(1, convId);
                ps2.executeUpdate();

                PreparedStatement ps3 = connection.prepareStatement("DELETE FROM conversation WHERE id = ?");
                ps3.setInt(1, convId);
                ps3.executeUpdate();

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

    @FXML
    private void handleManualMigration() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Migration manuelle");
        confirm.setHeaderText("Migrer les conversations existantes ?");
        confirm.setContentText("Cette opération va ajouter les propriétaires dans la table des membres.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String sql1 = "INSERT IGNORE INTO conversation_members (conversation_id, user_id, role, status, joined_at, invited_by) " +
                        "SELECT c.id, c.client_id, 'OWNER', 'ACTIVE', COALESCE(c.created_at, NOW()), c.client_id " +
                        "FROM conversation c";
                Statement st1 = connection.createStatement();
                int clientRows = st1.executeUpdate(sql1);

                String sql2 = "INSERT IGNORE INTO conversation_members (conversation_id, user_id, role, status, joined_at, invited_by) " +
                        "SELECT c.id, c.freelance_id, 'OWNER', 'ACTIVE', COALESCE(c.created_at, NOW()), c.freelance_id " +
                        "FROM conversation c";
                Statement st2 = connection.createStatement();
                int freelanceRows = st2.executeUpdate(sql2);

                showSuccess("✅ Migration terminée ! " + (clientRows + freelanceRows) + " membres ajoutés");
                migrationDone = true;
                loadConversations();

            } catch (SQLException e) {
                showError("❌ Erreur migration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ==================== GESTION DES MEMBRES ====================

    private void showMembersDialog(int convId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("👥 Gérer les membres");
        dialog.setHeaderText("Conversation #" + convId);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        // Récupérer les informations de la conversation
        Map<String, Object> convInfo = getConversationDetails(convId);
        String clientName = (String) convInfo.get("client_name");
        String freelanceName = (String) convInfo.get("freelance_name");
        String titre = (String) convInfo.get("titre");

        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10; -fx-background-radius: 5;");

        Label titleLabel = new Label(titre != null ? "📌 " + titre : "Conversation sans titre");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label participantsLabel = new Label("👤 Client: " + clientName + " | 👤 Freelance: " + freelanceName);
        participantsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        infoBox.getChildren().addAll(titleLabel, participantsLabel);

        // Liste des membres
        Label membersTitle = new Label("👥 Membres actuels :");
        membersTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0 5 0;");

        ListView<HBox> membersList = new ListView<>();
        membersList.setPrefHeight(250);
        membersList.setStyle("-fx-background-color: transparent;");

        loadMembersList(membersList, convId);

        // Section ajout
        VBox addSection = new VBox(10);
        addSection.setPadding(new Insets(10, 0, 0, 0));

        Label addTitle = new Label("➕ Ajouter un nouveau membre :");
        addTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> cmbUsers = new ComboBox<>();
        cmbUsers.setPromptText("Choisir un utilisateur");
        cmbUsers.setPrefWidth(300);
        cmbUsers.setStyle("-fx-background-radius: 5;");
        loadAvailableUsers(cmbUsers, convId);

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("MEMBER", "ADMIN");
        cmbRole.setValue("MEMBER");
        cmbRole.setPrefWidth(100);

        Button btnAdd = new Button("Ajouter");
        btnAdd.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        btnAdd.setOnMouseEntered(e -> btnAdd.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;"));
        btnAdd.setOnMouseExited(e -> btnAdd.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;"));

        addBox.getChildren().addAll(cmbUsers, cmbRole, btnAdd);

        addSection.getChildren().addAll(addTitle, addBox);

        content.getChildren().addAll(
                infoBox,
                new Separator(),
                membersTitle,
                membersList,
                new Separator(),
                addSection
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        btnAdd.setOnAction(e -> {
            String selectedUser = cmbUsers.getValue();
            String role = cmbRole.getValue();
            if (selectedUser != null && !selectedUser.isEmpty()) {
                addMember(convId, selectedUser, role);

                // Petite pause pour que la BD se mette à jour
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                        Platform.runLater(() -> {
                            // Recharger la liste des membres
                            loadMembersList(membersList, convId);
                            // Recharger la liste des utilisateurs disponibles
                            loadAvailableUsers(cmbUsers, convId);
                            // Forcer le rafraîchissement visuel
                            membersList.refresh();
                        });
                    } catch (InterruptedException ex) {}
                }).start();
            } else {
                showError("❌ Veuillez sélectionner un utilisateur");
            }
        });

        dialog.showAndWait();
    }

    private Map<String, Object> getConversationDetails(int convId) {
        Map<String, Object> details = new HashMap<>();
        try {
            String sql = "SELECT c.*, " +
                    "CONCAT(u1.first_name, ' ', u1.last_name) as client_name, " +
                    "CONCAT(u2.first_name, ' ', u2.last_name) as freelance_name " +
                    "FROM conversation c " +
                    "JOIN users u1 ON c.client_id = u1.id " +
                    "JOIN users u2 ON c.freelance_id = u2.id " +
                    "WHERE c.id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                details.put("client_name", rs.getString("client_name"));
                details.put("freelance_name", rs.getString("freelance_name"));
                details.put("titre", rs.getString("titre"));
                details.put("statut", rs.getString("statut"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement détails conversation");
        }
        return details;
    }

    private void loadMembersList(ListView<HBox> listView, int convId) {
        listView.getItems().clear();
        try {
            String sql = "SELECT cm.*, u.first_name, u.last_name, u.email, u.id as user_id " +
                    "FROM conversation_members cm " +
                    "JOIN users u ON cm.user_id = u.id " +
                    "WHERE cm.conversation_id = ? AND cm.status = 'ACTIVE' " +
                    "ORDER BY CASE cm.role " +
                    "   WHEN 'OWNER' THEN 1 " +
                    "   WHEN 'ADMIN' THEN 2 " +
                    "   ELSE 3 END, u.first_name ASC";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                HBox item = new HBox(10);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setPadding(new Insets(8));
                item.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 5; -fx-border-color: #e2e8f0; -fx-border-radius: 5;");

                int userId = rs.getInt("user_id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                String role = rs.getString("role");

                // Avatar avec couleur selon le rôle
                Label avatar = new Label(firstName.substring(0, 1).toUpperCase());
                String avatarColor = role.equals("OWNER") ? "#dc2626" :
                        role.equals("ADMIN") ? "#8b5cf6" : "#10b981";
                avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-text-fill: white; " +
                        "-fx-min-width: 35; -fx-min-height: 35; -fx-max-width: 35; -fx-max-height: 35; " +
                        "-fx-background-radius: 17.5; -fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 14;");

                // Informations
                VBox info = new VBox(2);
                Label nameLabel = new Label(firstName + " " + lastName);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

                String roleIcon = role.equals("OWNER") ? "👑 " :
                        role.equals("ADMIN") ? "⚙️ " : "👤 ";
                Label detailsLabel = new Label(roleIcon + role + " · " + email);
                detailsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
                info.getChildren().addAll(nameLabel, detailsLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Boutons d'action
                HBox actions = new HBox(5);

                if (!role.equals("OWNER") && userId != currentUserId) {
                    // Bouton changer rôle
                    Button btnRole = new Button("⚙️");
                    btnRole.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;");
                    btnRole.setTooltip(new Tooltip("Changer le rôle"));
                    btnRole.setOnAction(e -> showChangeRoleDialog(convId, userId, firstName + " " + lastName, role, listView));

                    // Bouton supprimer
                    Button btnDelete = new Button("❌");
                    btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;");
                    btnDelete.setTooltip(new Tooltip("Retirer le membre"));
                    btnDelete.setOnAction(e -> removeMember(convId, userId, firstName + " " + lastName, listView));

                    actions.getChildren().addAll(btnRole, btnDelete);
                } else if (userId == currentUserId) {
                    Label youLabel = new Label("(vous)");
                    youLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic; -fx-font-size: 12;");
                    actions.getChildren().add(youLabel);
                }

                item.getChildren().addAll(avatar, info, spacer, actions);
                listView.getItems().add(item);
            }

            System.out.println("📋 Membres chargés: " + listView.getItems().size());

        } catch (SQLException e) {
            e.printStackTrace();
            showError("❌ Erreur chargement des membres");
        }
    }

    private void loadAvailableUsers(ComboBox<String> combo, int convId) {
        combo.getItems().clear();
        try {
            String sql = "SELECT CONCAT(first_name, ' ', last_name, ' (', email, ')') as display, id " +
                    "FROM users WHERE id NOT IN " +
                    "(SELECT user_id FROM conversation_members WHERE conversation_id = ? AND status = 'ACTIVE') " +
                    "AND id != ? ORDER BY first_name";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, convId);
            ps.setInt(2, currentUserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                combo.getItems().add(rs.getString("display"));
            }

            if (combo.getItems().isEmpty()) {
                combo.setPromptText("Aucun utilisateur disponible");
                combo.setDisable(true);
            } else {
                combo.setDisable(false);
            }

            System.out.println("📋 Utilisateurs disponibles: " + combo.getItems().size());

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement utilisateurs");
        }
    }

    private void addMember(int convId, String selectedUser, String role) {
        try {
            // Extraire l'email de la chaîne "Prénom Nom (email)"
            String email = selectedUser.replaceAll(".*\\((.*)\\).*", "$1");

            // Récupérer l'ID de l'utilisateur
            String sql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");

                // Vérifier si l'utilisateur est déjà membre (même avec status 'LEFT')
                String checkSql = "SELECT status FROM conversation_members WHERE conversation_id = ? AND user_id = ?";
                PreparedStatement checkPs = connection.prepareStatement(checkSql);
                checkPs.setInt(1, convId);
                checkPs.setInt(2, userId);
                ResultSet checkRs = checkPs.executeQuery();

                if (checkRs.next()) {
                    // L'utilisateur existe déjà (peut-être avec status 'LEFT')
                    String currentStatus = checkRs.getString("status");

                    if ("LEFT".equals(currentStatus)) {
                        // Réactiver le membre
                        String updateSql = "UPDATE conversation_members SET status = 'ACTIVE', role = ?, invited_by = ?, joined_at = NOW() WHERE conversation_id = ? AND user_id = ?";
                        PreparedStatement updatePs = connection.prepareStatement(updateSql);
                        updatePs.setString(1, role);
                        updatePs.setInt(2, currentUserId);
                        updatePs.setInt(3, convId);
                        updatePs.setInt(4, userId);
                        updatePs.executeUpdate();

                        String systemMsg = "🔔 " + App.getCurrentUser().getFirstName() + " a réintégré " + selectedUser.split("\\(")[0].trim() + " à la conversation";
                        addSystemMessage(convId, systemMsg);
                        showSuccess("✅ Membre réintégré avec succès !");

                        // Vérification
                        System.out.println("✅ Membre réintégré - ID: " + userId + ", Rôle: " + role);
                    } else {
                        showError("❌ Cet utilisateur est déjà membre actif");
                        return;
                    }
                } else {
                    // Nouveau membre
                    String insert = "INSERT INTO conversation_members (conversation_id, user_id, role, invited_by, status, joined_at) VALUES (?, ?, ?, ?, 'ACTIVE', NOW())";
                    PreparedStatement psInsert = connection.prepareStatement(insert);
                    psInsert.setInt(1, convId);
                    psInsert.setInt(2, userId);
                    psInsert.setString(3, role);
                    psInsert.setInt(4, currentUserId);
                    psInsert.executeUpdate();

                    // Ajouter un message système dans la conversation
                    String systemMsg = "🔔 " + App.getCurrentUser().getFirstName() + " a ajouté " + selectedUser.split("\\(")[0].trim() + " à la conversation";
                    addSystemMessage(convId, systemMsg);
                    showSuccess("✅ Membre ajouté avec succès !");

                    // Vérification
                    System.out.println("✅ Nouveau membre ajouté - ID: " + userId + ", Rôle: " + role);
                }
            } else {
                showError("❌ Utilisateur non trouvé");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                showError("❌ Cet utilisateur est déjà membre");
            } else {
                showError("❌ Erreur ajout membre: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void removeMember(int convId, int userId, String userName, ListView<HBox> listView) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Retirer " + userName + " ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String sql = "UPDATE conversation_members SET status = 'LEFT' WHERE conversation_id = ? AND user_id = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, convId);
                ps.setInt(2, userId);
                ps.executeUpdate();

                // Ajouter un message système
                String systemMsg = "🔔 " + userName + " a été retiré de la conversation";
                addSystemMessage(convId, systemMsg);

                showSuccess("✅ " + userName + " a été retiré de la conversation");

                // Recharger la liste
                loadMembersList(listView, convId);

            } catch (SQLException e) {
                showError("❌ Erreur lors du retrait: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showChangeRoleDialog(int convId, int userId, String userName, String currentRole, ListView<HBox> listView) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le rôle");
        dialog.setHeaderText("Modifier le rôle de " + userName);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("MEMBER", "ADMIN");
        roleCombo.setValue(currentRole);
        roleCombo.setPrefWidth(200);

        content.getChildren().addAll(
                new Label("Nouveau rôle :"),
                roleCombo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return roleCombo.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newRole -> {
            if (!newRole.equals(currentRole)) {
                updateMemberRole(convId, userId, newRole, userName, listView);
            }
        });
    }

    private void updateMemberRole(int convId, int userId, String newRole, String userName, ListView<HBox> listView) {
        try {
            String sql = "UPDATE conversation_members SET role = ? WHERE conversation_id = ? AND user_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newRole);
            ps.setInt(2, convId);
            ps.setInt(3, userId);
            ps.executeUpdate();

            // Ajouter un message système
            String systemMsg = "🔔 " + userName + " est maintenant " + newRole;
            addSystemMessage(convId, systemMsg);

            showSuccess("✅ Rôle de " + userName + " modifié en " + newRole);

            // Recharger la liste
            loadMembersList(listView, convId);

        } catch (SQLException e) {
            showError("❌ Erreur modification rôle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addSystemMessage(int convId, String message) {
        try {
            String sql = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, type_message) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, message);
            ps.setString(2, "SYSTEM");
            ps.setInt(3, convId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "SYSTEM");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== UTILITAIRES ====================

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText("❌ " + message);
            errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            errorLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            errorLabel.setText("✅ " + message);
            errorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            errorLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
            }).start();
        });
    }
}