package com.khademni.controller;

import com.khademni.model.*;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import com.khademni.service.*;
import com.khademni.utils.EmailService;
import com.khademni.utils.SessionManager;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ArticleGUIController {

    @FXML
    private TableView<Article> tableArticles;
    @FXML
    private TableColumn<Article, String> colTitle;
    @FXML
    private TableColumn<Article, String> colContent;
    @FXML
    private TableColumn<Article, String> colStatus;
    @FXML
    private TableColumn<Article, LocalDateTime> colDate;
    @FXML
    private TableColumn<Article, Void> colActions;

    @FXML
    private PieChart pieChartFavoris;

    @FXML
    private PieChart pieChartCommentaires;

    @FXML
    private TextField txtTitle;
    @FXML
    private TextArea txtContent;
    @FXML
    private ComboBox<String> cmbStatus;
    @FXML
    private Label errorTitle;
    @FXML
    private Label errorContent;
    @FXML
    private Label errorStatus;

    @FXML
    private BarChart<String, Number> barChartFavoris;
    @FXML
    private CategoryAxis xAxisFavoris;
    @FXML
    private NumberAxis yAxisFavoris;

    @FXML
    private BarChart<String, Number> barChartCommentaires;
    @FXML
    private CategoryAxis xAxisCommentaires;
    @FXML
    private NumberAxis yAxisCommentaires;
    @FXML
    private javafx.scene.chart.PieChart pieChart;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblVisible;
    @FXML
    private Label lblMasque;
    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private VBox tableContainer;
    @FXML
    private VBox formContainer;
    @FXML
    private TextField searchField;
    @FXML
    private TextField txtImagePath;
    @FXML
    private VBox totalArticlesCard, visibleArticlesCard, hiddenArticlesCard;

    private final ArticleController articleController = new ArticleController();
    private Article articleSelectionne; // ✅ IMPORTANT

    private final FavoriController favoriService = new FavoriController();
    private final CommentaireController commentaireService = new CommentaireController();

    // ==================================================
    // INITIALIZE
    // ==================================================
    @FXML
    public void initialize() {

        // ComboBox
        cmbStatus.getItems().addAll("VISIBLE", "MASQUE");

        // Table columns

        initActionsColumn();
        loadArticles();

        // Click sur ligne => remplir formulaire
        tableArticles.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        remplirFormulaire(newVal);
                    }
                });
        // Table columns
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        tableArticles.setEditable(true);

        // Colorer le status
        colStatus.setCellFactory(column -> new TableCell<>() {

            private final Label badge = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                } else {

                    badge.setText(status);

                    if ("VISIBLE".equalsIgnoreCase(status)) {
                        badge.setStyle("""
                                    -fx-background-color: #dcfce7;
                                    -fx-text-fill: #16a34a;
                                    -fx-background-radius: 20;
                                    -fx-padding: 4 12;
                                    -fx-font-size: 11px;
                                    -fx-font-weight: bold;
                                """);
                    } else {
                        badge.setStyle("""
                                    -fx-background-color: #fee2e2;
                                    -fx-text-fill: #dc2626;
                                    -fx-background-radius: 20;
                                    -fx-padding: 4 12;
                                    -fx-font-size: 11px;
                                    -fx-font-weight: bold;
                                """);
                    }

                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        tableArticles.setEditable(true);

        colTitle.setCellFactory(column -> {
            TextFieldTableCell<Article, String> cell = new TextFieldTableCell<>();
            return cell;
        });

        colContent.setCellFactory(column -> {
            TextFieldTableCell<Article, String> cell = new TextFieldTableCell<>();
            return cell;
        });

        colTitle.setOnEditCommit(event -> {
            Article a = event.getRowValue();

            a.setTitle(event.getNewValue());

            try {
                articleController.update(a);
                loadStatistics();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Erreur lors de la mise à jour !");
            }
        });

        colContent.setOnEditCommit(event -> {
            Article a = event.getRowValue();
            a.setContent(event.getNewValue());

            try {
                articleController.update(a);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Erreur lors de la mise à jour !");
            }
        });

        colStatus.setOnEditCommit(event -> {
            Article a = event.getRowValue();
            a.setStatus(event.getNewValue());

            try {
                articleController.update(a);
                loadStatistics();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                        "Erreur",
                        "Erreur lors de la mise à jour !");
            }
        });

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String filter = newText.toLowerCase().trim();
            try {
                tableArticles.getItems().setAll(
                        articleController.findAll().stream()
                                .filter(a -> a.getTitle().toLowerCase().contains(filter))
                                .toList());
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        loadStatistics();

    }

    private void supprimerArticle(Article article) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'article");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet article ?");

        ButtonType btnOui = new ButtonType("Oui");
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        confirm.getButtonTypes().setAll(btnOui, btnNon);

        confirm.showAndWait().ifPresent(response -> {

            if (response == btnOui) {
                try {
                    articleController.delete(article.getId());
                    loadArticles();
                    loadStatistics();
                    clearForm();

                    // Envoi de l'email après la suppression de l'article
                    try {
                        UserModel currentUser = SessionManager.getCurrentUser();
                        if (currentUser != null) {
                            String userEmail = currentUser.getEmail();
                            EmailService.sendEmail(
                                    userEmail,
                                    "Article supprimé : " + article.getTitle(),
                                    "L'article suivant a été supprimé :\n\n" +
                                            "Titre : " + article.getTitle() + "\n" +
                                            "Contenu : " + article.getContent());
                            System.out.println("Email envoyé à : " + userEmail);
                        } else {
                            System.out.println("Aucun utilisateur connecté. Impossible d'envoyer l'email.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer l'email !");
                    }

                    showAlert(Alert.AlertType.INFORMATION,
                            "Succès",
                            "Article supprimé avec succès !");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR,
                            "Erreur",
                            "Erreur lors de la suppression !");
                }
            }
        });
    }

    private void loadStatistics() {
        try {
            var articles = articleController.findAll();

            System.out.println("Articles trouvés : " + articles.size());
            for (Article article : articles) {
                int favorisCount = favoriService.countByArticle(article.getId());
                int commentairesCount = commentaireService.countByArticle(article.getId());
                System.out.println("Article : " + article.getTitle() + ", Favoris : " + favorisCount
                        + ", Commentaires : " + commentairesCount);
            }

            // Vérifiez si les données sont bien récupérées
            if (articles.isEmpty()) {
                System.out.println("Aucun article trouvé !");
                return;
            }

            long visibleCount = articles.stream()
                    .filter(a -> "VISIBLE".equalsIgnoreCase(a.getStatus()))
                    .count();

            long masqueCount = articles.stream()
                    .filter(a -> "MASQUE".equalsIgnoreCase(a.getStatus()))
                    .count();

            long total = visibleCount + masqueCount;

            // Mettre à jour les Labels
            lblTotal.setText(String.valueOf(total));
            lblVisible.setText(String.valueOf(visibleCount));
            lblMasque.setText(String.valueOf(masqueCount));

            // ===== PIECHART =====
            pieChart.getData().clear();
            if (total > 0) {
                PieChart.Data visibleSlice = new PieChart.Data("VISIBLE (" + visibleCount + ")", visibleCount);
                PieChart.Data masqueSlice = new PieChart.Data("MASQUE (" + masqueCount + ")", masqueCount);

                pieChart.getData().addAll(visibleSlice, masqueSlice);

                visibleSlice.getNode().setStyle("-fx-pie-color: #5b21b6;");
                masqueSlice.getNode().setStyle("-fx-pie-color: #c4b5fd;");
            }

            // ===== BAR CHART FAVORIS =====
            barChartFavoris.getData().clear();
            xAxisFavoris.getCategories().clear();
            yAxisFavoris.setLabel("Nombre de Favoris");

            XYChart.Series<String, Number> favorisSeries = new XYChart.Series<>();
            favorisSeries.setName("Favoris");

            for (Article article : articles) {
                int favorisCount = favoriService.countByArticle(article.getId());
                if (favorisCount > 0) {
                    favorisSeries.getData().add(new XYChart.Data<>(article.getTitle(), favorisCount));
                }
            }

            barChartFavoris.getData().add(favorisSeries);

            // ===== BAR CHART COMMENTAIRES =====
            barChartCommentaires.getData().clear();
            xAxisCommentaires.getCategories().clear();
            yAxisCommentaires.setLabel("Nombre de Commentaires");
            yAxisCommentaires.setAutoRanging(true);
            yAxisCommentaires.setForceZeroInRange(true);
            XYChart.Series<String, Number> commentairesSeries = new XYChart.Series<>();
            commentairesSeries.setName("Commentaires");

            for (Article article : articles) {
                int commentairesCount = commentaireService.countByArticle(article.getId());
                if (commentairesCount > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(article.getTitle(), commentairesCount);
                    commentairesSeries.getData().add(data);

                    // Ajouter une étiquette sur chaque barre après que le nœud soit créé
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            Label label = new Label(data.getYValue().toString());
                            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c0df2; -fx-font-weight: bold;");
                            StackPane stackPane = (StackPane) newNode;
                            stackPane.getChildren().add(label);
                        }
                    });
                }
            }

            barChartCommentaires.getData().add(commentairesSeries);
            xAxisCommentaires.setTickLabelRotation(20); // Rotation des étiquettes pour une meilleure lisibilité

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================================================
    // ACTIONS COLUMN
    // ==================================================

    private void initActionsColumn() {

        colActions.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();

            {
                // ===== Bouton Modifier (icône moderne) =====
                FontIcon editIcon = new FontIcon("fas-pencil-alt");
                editIcon.setIconSize(16);
                btnEdit.setGraphic(editIcon);
                btnEdit.getStyleClass().add("btn-icon");

                btnEdit.setOnAction(e -> {
                    Article selected = getTableView().getItems().get(getIndex());
                    articleSelectionne = selected;
                    remplirFormulaire(selected);
                    showForm();
                });

                // ===== Bouton Supprimer (icône moderne) =====
                FontIcon deleteIcon = new FontIcon("fas-trash-alt");
                deleteIcon.setIconSize(16);
                btnDelete.setGraphic(deleteIcon);
                btnDelete.getStyleClass().add("btn-icon");

                btnDelete.setOnAction(e -> {
                    Article article = getTableView().getItems().get(getIndex());
                    supprimerArticle(article);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10, btnEdit, btnDelete);
                    setGraphic(box);
                }
            }
        });
    }

    // ==================================================
    // HELPERS
    // ==================================================
    private void remplirFormulaire(Article a) {
        articleSelectionne = a;
        txtTitle.setText(a.getTitle());
        txtContent.setText(a.getContent());
        cmbStatus.setValue(a.getStatus());
    }

    @FXML
    public void loadArticles() {
        try {
            tableArticles.getItems().setAll(articleController.findAll());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================================================
    // CRUD
    // ==================================================
    @FXML
    public void ajouterArticle() {
        if (!validateForm())
            return;

        try {
            if (articleSelectionne == null) {
                // ===== AJOUT =====
                Article article = new Article(
                        null,
                        txtTitle.getText(),
                        txtContent.getText(),
                        cmbStatus.getValue(),
                        LocalDateTime.now(),
                        txtImagePath.getText());

                System.out.println("Tentative d'ajout de l'article : " + article);

                articleController.create(article);

                System.out.println("Article ajouté avec succès dans la base de données.");

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Article ajouté avec succès !");
                // Envoi de l'email après l'ajout de l'article
                try {
                    UserModel currentUser = SessionManager.getCurrentUser();
                    if (currentUser != null) {
                        String userEmail = currentUser.getEmail();
                        EmailService.sendArticleCreationEmail(
                                userEmail,
                                article.getTitle(),
                                article.getContent(),
                                article.getCreatedAt().toString());
                        System.out.println("Email envoyé à : " + userEmail);
                    } else {
                        System.out.println("Aucun utilisateur connecté. Impossible d'envoyer l'email.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer l'email !");
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Article ajouté avec succès !");
            } else {
                // ===== MODIFICATION =====
                articleSelectionne.setTitle(txtTitle.getText());
                articleSelectionne.setContent(txtContent.getText());
                articleSelectionne.setStatus(cmbStatus.getValue());
                articleSelectionne.setImagePath(txtImagePath.getText());

                articleController.update(articleSelectionne);

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Article modifié avec succès !");
            }

            loadArticles();
            loadStatistics();
            hideForm();

            articleSelectionne = null;

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'opération !");
        }
    }

    @FXML
    public void modifierArticle() {

        if (articleSelectionne == null)
            return;
        if (!validateForm())
            return;

        try {
            articleSelectionne.setTitle(txtTitle.getText());
            articleSelectionne.setContent(txtContent.getText());
            articleSelectionne.setStatus(cmbStatus.getValue());
            articleSelectionne.setImagePath(txtImagePath.getText());

            articleController.update(articleSelectionne);

            // Envoi de l'email après la modification de l'article
            try {
                UserModel currentUser = SessionManager.getCurrentUser();
                if (currentUser != null) {
                    String userEmail = currentUser.getEmail();
                    EmailService.sendEmail(
                            userEmail,
                            "Article modifié : " + articleSelectionne.getTitle(),
                            "L'article suivant a été modifié :\n\n" +
                                    "Titre : " + articleSelectionne.getTitle() + "\n" +
                                    "Contenu : " + articleSelectionne.getContent() + "\n" +
                                    "Statut : " + articleSelectionne.getStatus());
                    System.out.println("Email envoyé à : " + userEmail);
                } else {
                    System.out.println("Aucun utilisateur connecté. Impossible d'envoyer l'email.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'envoyer l'email !");
            }

            loadArticles();
            loadStatistics();
            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Article modifié avec succès !");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification !");
        }
    }

    @FXML
    public void supprimerArticle() {

        if (articleSelectionne == null)
            return;

        supprimerArticle(articleSelectionne);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        txtTitle.clear();
        txtContent.clear();
        cmbStatus.setValue(null);
        articleSelectionne = null;
    }

    private boolean validateForm() {

        boolean isValid = true;

        errorTitle.setVisible(false);
        errorContent.setVisible(false);
        errorStatus.setVisible(false);

        String title = txtTitle.getText().trim();
        String content = txtContent.getText().trim();
        String imagePath = txtImagePath.getText().trim();
        // ----- TITLE -----
        if (title.isEmpty()) {
            errorTitle.setText("Le titre est obligatoire");
            errorTitle.setVisible(true);
            isValid = false;

        } else if (title.length() < 4) {
            errorTitle.setText("Le titre doit contenir au moins 4 caractères");
            errorTitle.setVisible(true);
            isValid = false;

        } else if (title.length() > 50) {
            errorTitle.setText("Le titre ne doit pas dépasser 50 caractères");
            errorTitle.setVisible(true);
            isValid = false;
        }

        // ----- CONTENT -----
        if (content.isEmpty()) {
            errorContent.setText("Le contenu est obligatoire");
            errorContent.setVisible(true);
            isValid = false;

        } else if (content.length() < 10) {
            errorContent.setText("Minimum 10 caractères");
            errorContent.setVisible(true);
            isValid = false;

        } else if (content.length() > 500) {
            errorContent.setText("Maximum 500 caractères");
            errorContent.setVisible(true);
            isValid = false;
        }

        // ----- STATUS -----
        if (cmbStatus.getValue() == null) {
            errorStatus.setText("Veuillez choisir un statut");
            errorStatus.setVisible(true);
            isValid = false;
        }
        // ----- IMAGE -----
        if (imagePath.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez choisir une image !");
            isValid = false;
        } else {
            File f = new File(imagePath);
            if (!f.exists() || f.isDirectory()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le fichier image est invalide !");
                isValid = false;
            } else {
                // Vérifier l'extension
                String ext = imagePath.substring(imagePath.lastIndexOf(".") + 1).toLowerCase();
                if (!(ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif"))) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "L'image doit être au format PNG, JPG, JPEG ou GIF !");
                    isValid = false;
                }
            }
        }
        if (!isValid) {
            System.out.println("Validation échouée. Vérifiez les champs du formulaire.");
        }
        return isValid;
    }

    @FXML
    public void showForm() {
        tableContainer.setVisible(false);
        tableContainer.setManaged(false);

        formContainer.setVisible(true);
        formContainer.setManaged(true);

        if (articleSelectionne == null) {
            clearForm();
        }
    }

    @FXML
    public void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);

        tableContainer.setVisible(true);
        tableContainer.setManaged(true);

        clearForm();
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(txtImagePath.getScene().getWindow());
        if (selectedFile != null) {
            txtImagePath.setText(selectedFile.getAbsolutePath());
        }
    }

}
