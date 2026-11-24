package com.campusrush.ui;

import com.campusrush.core.GameEngine;
import com.campusrush.db.DatabaseManager;
import com.campusrush.util.AppLogger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Subclass handling application lifecycle (init, start, stop).
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private GameEngine activeEngine;

    @Override
    public void init() throws Exception {
        AppLogger.info("Initializing Campus Rush application...");
        // Initialize SQLite DB schema and tables
        DatabaseManager.initializeDatabase();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            AppLogger.info("MainApp.start called, creating primary stage...");
            this.primaryStage = primaryStage;
            this.primaryStage.setTitle("Campus Rush - University Strategy & Race Game");
            this.primaryStage.setMinWidth(960);
            this.primaryStage.setMinHeight(680);

            showMainMenu();
            this.primaryStage.show();
            this.primaryStage.toFront();
            this.primaryStage.requestFocus();
            AppLogger.info("Primary stage shown successfully.");
        } catch (Throwable t) {
            AppLogger.severe("Exception in MainApp.start: " + t.getMessage(), t);
            t.printStackTrace();
        }
    }

    public void showMainMenu() {
        try {
            MainMenuView menuView = new MainMenuView(this);
            Scene scene = new Scene(menuView.getView(), 1050, 720);
            var cssUrl = getClass().getResource("/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            primaryStage.setScene(scene);
            AppLogger.info("Main menu scene loaded.");
        } catch (Throwable t) {
            AppLogger.severe("Exception in showMainMenu: " + t.getMessage(), t);
            t.printStackTrace();
        }
    }

    public void showMapView(GameEngine engine) {
        this.activeEngine = engine;
        MapView mapView = new MapView(this, engine);
        Scene scene = new Scene(mapView.getView(), 1300, 820);
        scene.setOnKeyPressed(event -> mapView.handleKeyPressed(event.getCode()));
        var cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setScene(scene);
    }

    public void showLeaderboard(GameEngine engine) {
        LeaderboardView view = new LeaderboardView(this, engine);
        Scene scene = new Scene(view.getView(), 960, 680);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showSessionHistory() {
        SessionHistoryView view = new SessionHistoryView(this);
        Scene scene = new Scene(view.getView(), 960, 680);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() throws Exception {
        AppLogger.info("Stopping Campus Rush application...");
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
