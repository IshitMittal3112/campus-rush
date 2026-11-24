package com.campusrush.ui;

import com.campusrush.core.GameEngine;
import com.campusrush.player.Player;
import com.campusrush.score.ScoreType;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Leaderboard View presenting final game session rankings.
 * "Winner is whoever has highest total score — not whoever finishes first."
 */
public class LeaderboardView {

    private final MainApp app;
    private final GameEngine engine;

    public LeaderboardView(MainApp app, GameEngine engine) {
        this.app = app;
        this.engine = engine;
    }

    public Parent getView() {
        VBox root = new VBox(20);
        root.getStyleClass().add("glass-panel");
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🏆 SESSION LEADERBOARD");
        titleLabel.getStyleClass().add("title-label");

        Label subtitleLabel = new Label("Highest total score wins! Tie-breaker: Missions Completed -> Least Time Penalty");
        subtitleLabel.getStyleClass().add("subtitle-label");

        TableView<Player> table = new TableView<>();
        table.setPrefHeight(250);

        TableColumn<Player, Integer> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(data -> {
            int rank = table.getItems().indexOf(data.getValue()) + 1;
            return new SimpleIntegerProperty(rank).asObject();
        });

        TableColumn<Player, String> nameCol = new TableColumn<>("Player Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Player, String> charCol = new TableColumn<>("Character Class");
        charCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCharacter().getName()));

        TableColumn<Player, Integer> scoreCol = new TableColumn<>("Total Score");
        scoreCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getScore()).asObject());

        TableColumn<Player, Integer> missionsCol = new TableColumn<>("Missions Done");
        missionsCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCompletedMissionsCount()).asObject());

        TableColumn<Player, Integer> hurdleCol = new TableColumn<>("Hurdles Solved");
        hurdleCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getHurdlesSolved()).asObject());

        TableColumn<Player, Integer> shortcutCol = new TableColumn<>("Shortcuts");
        shortcutCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getShortcutsTaken()).asObject());

        TableColumn<Player, Integer> firstCol = new TableColumn<>("First-Arrival Pts");
        firstCol.setCellValueFactory(data -> new SimpleIntegerProperty(
                engine.getScoreBoard().getBreakdown(data.getValue()).getOrDefault(ScoreType.FIRST_ARRIVAL, 0)).asObject());

        TableColumn<Player, Integer> penaltyCol = new TableColumn<>("Penalty Mins");
        penaltyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTotalTimePenalties()).asObject());

        table.getColumns().addAll(rankCol, nameCol, charCol, scoreCol, missionsCol, hurdleCol, shortcutCol, firstCol, penaltyCol);

        List<Player> leaderboard = engine.getScoreBoard().getLeaderboard(engine.getPlayers());
        table.getItems().addAll(leaderboard);

        Player winner = leaderboard.isEmpty() ? null : leaderboard.get(0);
        Label winnerLabel = new Label(winner != null ? "🎉 WINNER: " + winner.getName() + " with " + winner.getScore() + " Points!" : "");
        winnerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button menuBtn = new Button("Main Menu");
        menuBtn.getStyleClass().add("primary-button");
        menuBtn.setOnAction(e -> app.showMainMenu());

        Button historyBtn = new Button("Session History");
        historyBtn.setOnAction(e -> app.showSessionHistory());

        btnBox.getChildren().addAll(menuBtn, historyBtn);

        root.getChildren().addAll(titleLabel, subtitleLabel, winnerLabel, table, btnBox);
        return root;
    }
}
