package com.khademni.controller;

import com.khademni.model.ConversationModel;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

public class ConversationController {

    @FXML private TextField txtClientId;
    @FXML private TextField txtFreelanceId;
    @FXML private TableView<ConversationModel> tableConversations;
    @FXML private TableColumn<ConversationModel, Integer> colId;
    @FXML private TableColumn<ConversationModel, Integer> colClient;
    @FXML private TableColumn<ConversationModel, Integer> colFreelance;
    @FXML private TableColumn<ConversationModel, String> colStatut;
    @FXML private Label errorLabel;

    // Nouveaux boutons pour UPDATE
    @FXML private Button btnArchiver;
    @FXML private Button btnActiver;
    @FXML private ComboBox<String> cmbStatut;

    private Connection connection;
    private ObservableList<ConversationModel> conversationList = FXCollections.observableArrayList();

    public ConversationController() {
        try {
            connection = MyDataBase.getConnection();
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

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFreelance.setCellValueFactory(new PropertyValueFactory<>("freelanceId"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Style pour la colonne statut
        colStatut.setCellFactory(column -> new TableCell<ConversationModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equals(item)) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else if ("INACTIVE".equals(item)) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Initialiser la ComboBox pour le statut
        cmbStatut.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));

        // Désactiver les boutons UPDATE si aucune ligne sélectionnée
        btnArchiver.setDisable(true);
        btnActiver.setDisable(true);
        cmbStatut.setDisable(true);

        // Écouter la sélection dans le tableau
        tableConversations.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        btnArchiver.setDisable(false);
                        btnActiver.setDisable(false);
                        cmbStatut.setDisable(false);
                        cmbStatut.setValue(newSelection.getStatut());
                    } else {
                        btnArchiver.setDisable(true);
                        btnActiver.setDisable(true);
                        cmbStatut.setDisable(true);
                    }
                }
        );

        refreshTable();
    }

    @FXML
    private void handleOpenChat() {
        ConversationModel selected = tableConversations.getSelectionModel().getSelectedItem();

        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Message.fxml"));
                Parent root = loader.load();

                MessageController messageController = loader.getController();
                messageController.setConversationId(selected.getId());
                messageController.setConversationInfo(selected.getClientId(), selected.getFreelanceId());

                Stage stage = (Stage) tableConversations.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Conversation #" + selected.getId());

            } catch (IOException e) {
                showError("Impossible de charger l'interface de chat.");
                e.printStackTrace();
            }
        } else {
            showError("Veuillez sélectionner une conversation dans le tableau.");
        }
    }

    @FXML
    private void handleAjouter() {
        errorLabel.setVisible(false);

        try {
            if (txtClientId.getText().trim().isEmpty() || txtFreelanceId.getText().trim().isEmpty()) {
                showError("Veuillez remplir tous les champs.");
                return;
            }

            int clientId = Integer.parseInt(txtClientId.getText().trim());
            int freelanceId = Integer.parseInt(txtFreelanceId.getText().trim());

            if (clientId == freelanceId) {
                showError("Un utilisateur ne peut pas se contacter lui-même.");
                return;
            }

            if (!userExists(clientId)) {
                showError("L'utilisateur avec l'ID " + clientId + " n'existe pas.");
                return;
            }

            if (!userExists(freelanceId)) {
                showError("L'utilisateur avec l'ID " + freelanceId + " n'existe pas.");
                return;
            }

            if (conversationExists(clientId, freelanceId)) {
                showError("Une conversation existe déjà entre ces utilisateurs.");
                return;
            }

            String req = "INSERT INTO conversation (client_id, freelance_id, statut, date_creation) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, clientId);
            ps.setInt(2, freelanceId);
            ps.setString(3, "ACTIVE");
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    showSuccess("Conversation créée avec succès ! ID: " + newId);
                }
                refreshTable();
                clearFields();
            }

        } catch (NumberFormatException e) {
            showError("Les IDs doivent être des nombres.");
        } catch (SQLException e) {
            showError("Erreur lors de l'accès à la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ MÉTHODES UPDATE ============

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
        ConversationModel selected = tableConversations.getSelectionModel().getSelectedItem();
        if (selected != null && cmbStatut.getValue() != null) {
            updateStatut(cmbStatut.getValue());
        }
    }

    private void updateStatut(String nouveauStatut) {
        ConversationModel selected = tableConversations.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Veuillez sélectionner une conversation.");
            return;
        }

        if (selected.getStatut().equals(nouveauStatut)) {
            showError("La conversation est déjà " + nouveauStatut);
            return;
        }

        try {
            String req = "UPDATE conversation SET statut = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, nouveauStatut);
            ps.setInt(2, selected.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                showSuccess("Statut modifié avec succès : " + nouveauStatut);
                refreshTable();
            }

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // ============ MÉTHODE DELETE CORRIGÉE ============

    @FXML
    private void handleSupprimer() {
        ConversationModel selected = tableConversations.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Veuillez sélectionner une conversation à supprimer.");
            return;
        }

        // Boîte de dialogue de confirmation avec plus de détails
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la conversation #" + selected.getId());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette conversation ?\n\n" +
                "Client: " + selected.getClientId() + "\n" +
                "Freelance: " + selected.getFreelanceId() + "\n" +
                "Statut: " + selected.getStatut() + "\n\n" +
                "⚠️ Tous les messages associés seront également supprimés !");

        // Personnaliser les boutons
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnNon);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnOui) {
                try {
                    // Désactiver l'auto-commit pour transaction
                    connection.setAutoCommit(false);

                    try {
                        // 1. Supprimer d'abord les messages associés
                        String deleteMessages = "DELETE FROM message WHERE conversation_id = ?";
                        PreparedStatement ps1 = connection.prepareStatement(deleteMessages);
                        ps1.setInt(1, selected.getId());
                        int messagesDeleted = ps1.executeUpdate();
                        System.out.println(messagesDeleted + " messages supprimés");

                        // 2. Supprimer la conversation
                        String deleteConversation = "DELETE FROM conversation WHERE id = ?";
                        PreparedStatement ps2 = connection.prepareStatement(deleteConversation);
                        ps2.setInt(1, selected.getId());
                        int conversationDeleted = ps2.executeUpdate();

                        // Valider la transaction
                        connection.commit();

                        if (conversationDeleted > 0) {
                            showSuccess("Conversation supprimée avec succès !");
                            refreshTable();
                        }

                    } catch (SQLException e) {
                        // En cas d'erreur, annuler la transaction
                        connection.rollback();
                        throw e;
                    } finally {
                        // Réactiver l'auto-commit
                        connection.setAutoCommit(true);
                    }

                } catch (SQLException e) {
                    showError("Erreur lors de la suppression: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean userExists(int id) throws SQLException {
        String req = "SELECT id FROM users WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private boolean conversationExists(int clientId, int freelanceId) throws SQLException {
        String req = "SELECT id FROM conversation WHERE client_id = ? AND freelance_id = ? AND statut = 'ACTIVE'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, clientId);
        ps.setInt(2, freelanceId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private void refreshTable() {
        conversationList.clear();
        try {
            String req = "SELECT * FROM conversation ORDER BY date_creation DESC";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                ConversationModel conversation = new ConversationModel(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("freelance_id"),
                        rs.getString("statut")
                );

                Timestamp timestamp = rs.getTimestamp("date_creation");
                if (timestamp != null) {
                    conversation.setDateCreation(timestamp.toLocalDateTime());
                }

                conversationList.add(conversation);
            }
            tableConversations.setItems(conversationList);

        } catch (SQLException e) {
            showError("Erreur lors du chargement des conversations: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        // Auto-cacher après 4 secondes
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void showSuccess(String message) {
        errorLabel.setText("✅ " + message);
        errorLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void clearFields() {
        txtClientId.clear();
        txtFreelanceId.clear();
    }
}