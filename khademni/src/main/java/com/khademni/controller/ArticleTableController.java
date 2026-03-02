package com.khademni.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import com.khademni.model.Article;

import java.time.LocalDateTime;

public class ArticleTableController {
    @FXML
    private TableView<Article> tableArticles;
    @FXML private TableColumn<Article, Integer> colId;
    @FXML private TableColumn<Article, String> colTitle;
    @FXML private TableColumn<Article, String> colContent;
    @FXML private TableColumn<Article, String> colStatus;
    @FXML private TableColumn<Article, LocalDateTime> colDate;
}
