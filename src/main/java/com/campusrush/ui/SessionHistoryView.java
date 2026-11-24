package com.campusrush.ui;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.db.GameSessionDao;
import com.campusrush.db.GameSessionDao.SessionRecord;
import com.campusrush.util.AppLogger;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Historical Session Log Screen displaying past games persisted in SQLite DB.
 * Demonstrates load-bearing JDBC integration.
 */
public class SessionHistoryView {

    private final MainApp app;

    public SessionHistoryView(MainApp app) {
        this.app = app;
    }

    public Parent getView() {
        VBox root = new VBox(20);
        root.getStyleClass().add("glass-panel");
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("📜 PERSISTED SESSION HISTORY");
        titleLabel.getStyleClass().add("title-label");

        Label subtitleLabel = new Label("Loaded directly from SQLite Database (campusrush.db)");
        subtitleLabel.getStyleClass().add("subtitle-label");

        TableView<SessionRecord> table = new TableView<>();
        table.setPrefHeight(300);

        TableColumn<SessionRecord, Integer> idCol = new TableColumn<>("Session ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<SessionRecord, String> startCol = new TableColumn<>("Started At");
        startCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStartedAt()));

        TableColumn<SessionRecord, String> endCol = new TableColumn<>("Ended At");
        endCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndedAt()));

        TableColumn<SessionRecord, Integer> durCol = new TableColumn<>("Duration (Mins)");
        durCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDurationMinutes()).asObject());

        TableColumn<SessionRecord, String> winnerCol = new TableColumn<>("Winner");
        winnerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getWinner()));

        TableColumn<SessionRecord, Integer> topCol = new TableColumn<>("Winning Score");
        topCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTopScore()).asObject());

        table.getColumns().addAll(idCol, startCol, endCol, durCol, winnerCol, topCol);

        try {
            GameSessionDao dao = new GameSessionDao();
            List<SessionRecord> records = dao.findAllSessions();
            table.getItems().addAll(records);
        } catch (DataAccessException e) {
            AppLogger.severe("Failed to load historical session records: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Database Error: " + e.getMessage());
            alert.show();
        }

        Button backBtn = new Button("Back to Main Menu");
        backBtn.getStyleClass().add("primary-button");
        backBtn.setOnAction(e -> app.showMainMenu());

        root.getChildren().addAll(titleLabel, subtitleLabel, table, backBtn);
        return root;
    }
}
