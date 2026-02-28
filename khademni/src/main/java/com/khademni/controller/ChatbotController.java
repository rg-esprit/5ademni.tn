package com.khademni.controller;

import com.khademni.App;
import com.khademni.service.ChatbotService;
import com.khademni.service.ChatbotService.ChatResponse;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;

import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur du chatbot flottant.
 * Crée programmatiquement un widget de chat glassmorphism (FAB + panneau
 * coulissant).
 */
public class ChatbotController {

    private final ChatbotService chatbotService = new ChatbotService();
    private final StackPane overlay;
    private final VBox chatPanel;
    private VBox messagesContainer;
    private ScrollPane scrollPane;
    private final StackPane fabButton;
    private boolean isOpen = false;

    private static final double PANEL_WIDTH = 400;
    private static final double PANEL_HEIGHT = 520;
    private static final double FAB_SIZE = 56;

    public ChatbotController() {
        this.overlay = new StackPane();
        this.overlay.setPickOnBounds(false);
        this.overlay.setMouseTransparent(false);

        // ── FAB Button ───────────────────────────────────────────────────
        fabButton = createFAB();

        // ── Chat Panel ───────────────────────────────────────────────────
        chatPanel = createChatPanel();
        chatPanel.setVisible(false);
        chatPanel.setOpacity(0);

        // Position both at bottom-right
        StackPane.setAlignment(fabButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fabButton, new Insets(0, 28, 28, 0));

        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 28, 96, 0));

        overlay.getChildren().addAll(chatPanel, fabButton);

        // Load CSS
        try {
            String css = App.class.getResource("chatbot.css").toExternalForm();
            overlay.getStylesheets().add(css);
        } catch (Exception e) {
            // CSS not loaded — graceful fallback
        }
    }

    /**
     * @return the overlay StackPane to add to the scene root.
     */
    public StackPane getOverlay() {
        return overlay;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FAB (Floating Action Button)
    // ═══════════════════════════════════════════════════════════════════════

    private StackPane createFAB() {
        StackPane fab = new StackPane();
        fab.setPrefSize(FAB_SIZE, FAB_SIZE);
        fab.setMaxSize(FAB_SIZE, FAB_SIZE);
        fab.setMinSize(FAB_SIZE, FAB_SIZE);

        // Circle background
        Circle circle = new Circle(FAB_SIZE / 2);
        circle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6c0df2")),
                new Stop(1, Color.web("#a855f7"))));
        circle.setEffect(new DropShadow(20, 0, 6, Color.rgb(108, 13, 242, 0.45)));

        // Chat icon (💬 emoji as text)
        Label icon = new Label("💬");
        icon.setStyle("-fx-font-size: 24px;");

        fab.getChildren().addAll(circle, icon);
        fab.setCursor(javafx.scene.Cursor.HAND);
        fab.getStyleClass().add("chatbot-fab");

        // Hover animation
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), fab);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), fab);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        fab.setOnMouseEntered(e -> scaleUp.playFromStart());
        fab.setOnMouseExited(e -> scaleDown.playFromStart());
        fab.setOnMouseClicked(e -> toggleChat());

        // Pulse animation on idle
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), fab);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.06);
        pulse.setToY(1.06);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        return fab;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Chat Panel
    // ═══════════════════════════════════════════════════════════════════════

    private VBox createChatPanel() {
        VBox panel = new VBox(0);
        panel.setPrefSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.setMaxSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.setMinSize(PANEL_WIDTH, PANEL_HEIGHT);
        panel.getStyleClass().add("chatbot-panel");

        // ── Header ───────────────────────────────────────────────────────
        HBox header = createHeader();

        // ── Messages Area ────────────────────────────────────────────────
        messagesContainer = new VBox(12);
        messagesContainer.setPadding(new Insets(16));
        messagesContainer.getStyleClass().add("chatbot-messages");

        scrollPane = new ScrollPane(messagesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("chatbot-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Input Bar ────────────────────────────────────────────────────
        HBox inputBar = createInputBar();

        // ── Quick Actions ────────────────────────────────────────────────
        HBox quickActions = createQuickActions();

        panel.getChildren().addAll(header, scrollPane, quickActions, inputBar);

        // Clip for rounded corners
        Rectangle clip = new Rectangle(PANEL_WIDTH, PANEL_HEIGHT);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        panel.setClip(clip);

        return panel;
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.getStyleClass().add("chatbot-header");

        // Bot avatar
        StackPane avatar = new StackPane();
        Circle avatarBg = new Circle(18);
        avatarBg.setFill(Color.web("#f3f0ff"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 18px;");
        avatar.getChildren().addAll(avatarBg, avatarIcon);

        // Title
        VBox titleBox = new VBox(2);
        Label title = new Label("Assistant 5ademni");
        title.getStyleClass().add("chatbot-title");
        Label status = new Label("● En ligne");
        status.getStyleClass().add("chatbot-status");
        titleBox.getChildren().addAll(title, status);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("chatbot-close-btn");
        closeBtn.setOnAction(e -> toggleChat());

        header.getChildren().addAll(avatar, titleBox, closeBtn);
        return header;
    }

    private HBox createInputBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12, 16, 16, 16));
        bar.getStyleClass().add("chatbot-input-bar");

        TextField input = new TextField();
        input.setPromptText("Tapez votre message...");
        input.getStyleClass().add("chatbot-input");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.getStyleClass().add("chatbot-send-btn");

        Runnable sendAction = () -> {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                addUserMessage(text);
                input.clear();
                processUserMessage(text);
            }
        };

        sendBtn.setOnAction(e -> sendAction.run());
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)
                sendAction.run();
        });

        bar.getChildren().addAll(input, sendBtn);
        return bar;
    }

    private HBox createQuickActions() {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(4, 16, 8, 16));

        String[][] quickBtns = {
                { "📊", "Stats" },
                { "📄", "Rapport PDF" },
                { "🎓", "Guide" },
                { "🔒", "Sécurité" }
        };

        String[] quickCommands = {
                "Combien d'offres ont été créées ?",
                "Génère mon rapport",
                "Comment devenir freelancer ?",
                "Le paiement est sûr ?"
        };

        for (int i = 0; i < quickBtns.length; i++) {
            Button btn = new Button(quickBtns[i][0] + " " + quickBtns[i][1]);
            btn.getStyleClass().add("chatbot-quick-btn");
            final int idx = i;
            btn.setOnAction(e -> {
                addUserMessage(quickCommands[idx]);
                processUserMessage(quickCommands[idx]);
            });
            actions.getChildren().add(btn);
        }

        return actions;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Chat Logic
    // ═══════════════════════════════════════════════════════════════════════

    private void toggleChat() {
        if (isOpen) {
            closeChat();
        } else {
            openChat();
        }
    }

    private void openChat() {
        isOpen = true;
        chatPanel.setVisible(true);
        chatPanel.setTranslateY(30);

        // Fade + slide animation
        FadeTransition fade = new FadeTransition(Duration.millis(250), chatPanel);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), chatPanel);
        slide.setFromY(30);
        slide.setToY(0);

        ParallelTransition open = new ParallelTransition(fade, slide);
        open.setInterpolator(Interpolator.EASE_OUT);
        open.play();

        // Send welcome if first open
        if (messagesContainer.getChildren().isEmpty()) {
            addBotMessageDelayed("👋 Bienvenue sur 5ademni.tn !\n\n"
                    + "Je suis votre assistant intelligent. Comment puis-je vous aider ?\n\n"
                    + "Utilisez les boutons rapides ci-dessous ou tapez votre question !", 400);
        }
    }

    private void closeChat() {
        isOpen = false;
        FadeTransition fade = new FadeTransition(Duration.millis(200), chatPanel);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), chatPanel);
        slide.setFromY(0);
        slide.setToY(20);

        ParallelTransition close = new ParallelTransition(fade, slide);
        close.setInterpolator(Interpolator.EASE_IN);
        close.setOnFinished(e -> chatPanel.setVisible(false));
        close.play();
    }

    private void addUserMessage(String text) {
        HBox bubble = new HBox();
        bubble.setAlignment(Pos.CENTER_RIGHT);
        bubble.setPadding(new Insets(0, 0, 0, 60));

        VBox msgBox = new VBox(4);
        msgBox.getStyleClass().add("chatbot-user-bubble");
        msgBox.setPadding(new Insets(10, 14, 10, 14));
        msgBox.setMaxWidth(280);

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("chatbot-user-text");

        Label timeLabel = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("chatbot-time-user");

        msgBox.getChildren().addAll(msgLabel, timeLabel);
        bubble.getChildren().add(msgBox);

        messagesContainer.getChildren().add(bubble);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox bubble = new HBox(8);
        bubble.setAlignment(Pos.TOP_LEFT);
        bubble.setPadding(new Insets(0, 60, 0, 0));

        // Bot avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        Circle avatarBg = new Circle(16);
        avatarBg.setFill(Color.web("#f3f0ff"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 14px;");
        avatar.getChildren().addAll(avatarBg, avatarIcon);

        VBox msgBox = new VBox(4);
        msgBox.getStyleClass().add("chatbot-bot-bubble");
        msgBox.setPadding(new Insets(10, 14, 10, 14));
        msgBox.setMaxWidth(280);

        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("chatbot-bot-text");

        Label timeLabel = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("chatbot-time-bot");

        msgBox.getChildren().addAll(msgLabel, timeLabel);
        bubble.getChildren().addAll(avatar, msgBox);

        // Fade-in animation
        bubble.setOpacity(0);
        messagesContainer.getChildren().add(bubble);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bubble);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        scrollToBottom();
    }

    private void addBotMessageDelayed(String text, int delayMs) {
        // Show typing indicator
        HBox typingBubble = createTypingIndicator();
        messagesContainer.getChildren().add(typingBubble);
        scrollToBottom();

        PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
        delay.setOnFinished(e -> {
            messagesContainer.getChildren().remove(typingBubble);
            addBotMessage(text);
        });
        delay.play();
    }

    private void addStatsBotMessage(ChatResponse response) {
        HBox bubble = new HBox(8);
        bubble.setAlignment(Pos.TOP_LEFT);
        bubble.setPadding(new Insets(0, 40, 0, 0));

        // Bot avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        Circle avatarBg = new Circle(16);
        avatarBg.setFill(Color.web("#f3f0ff"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 14px;");
        avatar.getChildren().addAll(avatarBg, avatarIcon);

        VBox msgBox = new VBox(10);
        msgBox.getStyleClass().add("chatbot-bot-bubble");
        msgBox.setPadding(new Insets(12, 14, 12, 14));
        msgBox.setMaxWidth(300);

        Label header = new Label("📊 Statistiques en temps réel");
        header.setWrapText(true);
        header.getStyleClass().add("chatbot-bot-text");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // Stats badges
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                createStatBadge("📦", String.valueOf(response.getOffresCount()), "Offres", "#6c0df2"),
                createStatBadge("📋", String.valueOf(response.getDemandesCount()), "Demandes", "#10b981"),
                createStatBadge("📄", String.valueOf(response.getCvCount()), "CVs", "#f59e0b"));

        Label timeLabel = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("chatbot-time-bot");

        msgBox.getChildren().addAll(header, badges, timeLabel);
        bubble.getChildren().addAll(avatar, msgBox);

        bubble.setOpacity(0);
        messagesContainer.getChildren().add(bubble);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), bubble);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        scrollToBottom();
    }

    private VBox createStatBadge(String emoji, String value, String label, String color) {
        VBox badge = new VBox(2);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(8, 12, 8, 12));
        badge.setStyle("-fx-background-color: " + color + "14; -fx-background-radius: 10; -fx-border-color: " + color
                + "33; -fx-border-radius: 10; -fx-border-width: 1;");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 16px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

        badge.getChildren().addAll(emojiLabel, valueLabel, nameLabel);
        return badge;
    }

    private HBox createTypingIndicator() {
        HBox bubble = new HBox(8);
        bubble.setAlignment(Pos.TOP_LEFT);
        bubble.setPadding(new Insets(0, 60, 0, 0));

        StackPane avatar = new StackPane();
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        Circle avatarBg = new Circle(16);
        avatarBg.setFill(Color.web("#f3f0ff"));
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 14px;");
        avatar.getChildren().addAll(avatarBg, avatarIcon);

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);
        dots.setPadding(new Insets(12, 18, 12, 18));
        dots.getStyleClass().add("chatbot-bot-bubble");

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.setFill(Color.web("#9ca3af"));

            TranslateTransition bounce = new TranslateTransition(Duration.millis(400), dot);
            bounce.setFromY(0);
            bounce.setToY(-6);
            bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setAutoReverse(true);
            bounce.setDelay(Duration.millis(i * 150));
            bounce.play();

            dots.getChildren().add(dot);
        }

        bubble.getChildren().addAll(avatar, dots);
        return bubble;
    }

    private void processUserMessage(String text) {
        // Show typing indicator, then respond
        HBox typingBubble = createTypingIndicator();
        messagesContainer.getChildren().add(typingBubble);
        scrollToBottom();

        PauseTransition delay = new PauseTransition(Duration.millis(800));
        delay.setOnFinished(e -> {
            messagesContainer.getChildren().remove(typingBubble);

            ChatResponse response = chatbotService.processMessage(text);

            if (response.getIntent() == ChatbotService.Intent.STATS
                    && response.getOffresCount() + response.getDemandesCount() + response.getCvCount() > 0) {
                addStatsBotMessage(response);
            } else {
                addBotMessage(response.getMessage());
            }
        });
        delay.play();
    }

    private void scrollToBottom() {
        javafx.application.Platform.runLater(() -> {
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        });
    }
}
