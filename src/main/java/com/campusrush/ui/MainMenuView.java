package com.campusrush.ui;

import com.campusrush.core.GameEngine;
import com.campusrush.map.CampusMap;
import com.campusrush.map.Location;
import com.campusrush.map.MapLoader;
import com.campusrush.player.*;
import com.campusrush.player.Character;
import com.campusrush.util.AppLogger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Main Menu View for player setup, character selection, game startup, and session load.
 */
public class MainMenuView {

    private final MainApp app;
    private final VBox playerSetupBox = new VBox(10);
    private final List<TextField> nameFields = new ArrayList<>();
    private final List<ComboBox<String>> charCombos = new ArrayList<>();
    private final VBox playerRowsBox = new VBox(10);
    private final Label classInfo = new Label();

    public MainMenuView(MainApp app) {
        this.app = app;
    }

    public Parent getView() {
        VBox root = new VBox(25);
        root.getStyleClass().add("glass-panel");
        root.setPadding(new Insets(35));
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(600);

        Label titleLabel = new Label("🎓 CAMPUS RUSH");
        titleLabel.getStyleClass().add("title-label");

        Label subtitle = new Label("University Strategy, Navigation & Survival Challenge");
        subtitle.getStyleClass().add("subtitle-label");

        playerSetupBox.setAlignment(Pos.CENTER);
        ComboBox<Integer> playerCount = new ComboBox<>();
        playerCount.getItems().addAll(1, 2, 3, 4);
        playerCount.setValue(1);
        playerCount.setOnAction(e -> rebuildPlayerInputs(playerCount.getValue()));
        HBox countRow = new HBox(10, new Label("Players (local hot-seat):"), playerCount);
        countRow.setAlignment(Pos.CENTER);
        classInfo.setWrapText(true);
        classInfo.setMaxWidth(430);
        classInfo.setStyle("-fx-text-fill: #9fd8ff; -fx-font-size: 12px; -fx-text-alignment: center;");
        playerSetupBox.getChildren().addAll(countRow, playerRowsBox, classInfo);
        rebuildPlayerInputs(1);
        refreshClassInfo();

        // Action Buttons
        VBox btnBox = new VBox(12);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setMaxWidth(300);

        Button startBtn = new Button("🚀 Start New Game");
        startBtn.getStyleClass().add("primary-button");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> startNewGame());

        Button loadBtn = new Button("📂 Load Saved Game");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.setOnAction(e -> loadSavedGame());

        Button historyBtn = new Button("📜 Session History");
        historyBtn.setMaxWidth(Double.MAX_VALUE);
        historyBtn.setOnAction(e -> app.showSessionHistory());

        btnBox.getChildren().addAll(startBtn, loadBtn, historyBtn);

        root.getChildren().addAll(titleLabel, subtitle, playerSetupBox, btnBox);

        StackPane centerWrapper = new StackPane(root);
        centerWrapper.setPadding(new Insets(40));
        centerWrapper.setStyle("-fx-background-color: #12121c;");
        return centerWrapper;
    }

    private void rebuildPlayerInputs(int count) {
        playerRowsBox.getChildren().clear();
        nameFields.clear();
        charCombos.clear();

        String[] defaultNames = {"Alex", "Jordan", "Taylor", "Morgan"};

        for (int i = 0; i < count; i++) {
            final int playerIndex = i;
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER);

            Label pLabel = new Label("Player " + (i + 1) + ":");
            pLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #00f2fe;");

            TextField nameField = new TextField(defaultNames[i % defaultNames.length]);
            nameField.setPromptText("Player Name");
            nameFields.add(nameField);

            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll(Characters.NAMES);
            combo.setTooltip(new Tooltip("Sprinter: faster walking | Scholar: hurdle questions worth double | Negotiator: fewer hurdles"));
            combo.getSelectionModel().select(i % 3);
            charCombos.add(combo);
            combo.valueProperty().addListener((o, a, b) -> { if (playerIndex == 0) refreshClassInfo(); });

            row.getChildren().addAll(pLabel, nameField, combo);
            playerRowsBox.getChildren().add(row);
        }
        refreshClassInfo();
    }

    private void refreshClassInfo() {
        if (!charCombos.isEmpty() && charCombos.get(0).getValue() != null) {
            classInfo.setText("🎓 " + Characters.byName(charCombos.get(0).getValue()).getDescription());
        }
    }

    private void startNewGame() {
        CampusMap map = MapLoader.loadMapFromCSV("campus_map.csv");
        Location startLoc = map.getLocation("MAIN_GATE");
        if (startLoc == null) {
            startLoc = map.getAllLocations().iterator().next();
        }

        List<Player> players = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (int i = 0; i < nameFields.size(); i++) {
            String name = nameFields.get(i).getText().trim();
            if (name.isEmpty()) name = "Player " + (i + 1);
            while (!used.add(name.toLowerCase())) name = name + " " + (i + 1);   // names must be unique

            Character character = Characters.byName(charCombos.get(i).getValue());
            players.add(new Player(i + 1, name, character, startLoc));
        }

        GameEngine engine = new GameEngine(players, map);
        for (String problem : engine.validateMissions()) AppLogger.warning("Mission data problem: " + problem);
        app.showMapView(engine);
    }

    private void loadSavedGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Campus Rush Save File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Campus Rush Save (*.sav)", "*.sav"));
        File file = fileChooser.showOpenDialog(app.getPrimaryStage());

        if (file != null) {
            try {
                GameEngine engine = GameEngine.loadGameSession(file);
                app.showMapView(engine);
            } catch (Exception ex) {
                AppLogger.severe("Failed to load saved game: " + ex.getMessage(), ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load save file: " + ex.getMessage());
                alert.show();
            }
        }
    }
}
