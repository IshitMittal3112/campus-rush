package com.campusrush.ui;

import com.campusrush.map.Route;
import com.campusrush.mission.Mission;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modal dialog listing the routes found by the Dijkstra pathfinder for the player's clock and class.
 * Each route shows its estimated arrival against the mission window, so the player can weigh
 * a fast risky shortcut against a longer safe road.
 */
public class RouteSelectionDialog {

    /** @return the chosen route, or null if the player cancelled. */
    public static Route showDialog(Stage owner, List<Route> routes, Mission mission, LocalDateTime now) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Choose Your Route Across Campus");

        VBox root = new VBox(12);
        root.getStyleClass().add("glass-panel");
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Route to " + mission.getDestination().getName());
        titleLabel.getStyleClass().add("section-header");

        Label windowLabel = new Label("Mission window " + mission.windowText() + "  |  now "
                + now.toLocalTime().withSecond(0).withNano(0) + "  |  reward " + mission.getRewardPoints() + " pts");
        windowLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");

        ListView<Route> listView = new ListView<>();
        listView.getItems().addAll(routes);
        listView.setPrefHeight(220);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Route r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setText(null); return; }
                LocalDateTime eta = now.plusSeconds(Math.round(r.getEstimatedMinutes() * 60));
                boolean late = mission.isPastDeadline(eta);
                setText(r.getLabel() + (r.usesShortcut() ? "  (uses a shortcut)" : "") + "\n"
                        + String.format("~%.0f min | arrive %s %s | hurdle chance %d%%",
                        r.getEstimatedMinutes(), eta.toLocalTime().withSecond(0).withNano(0),
                        late ? "(TOO LATE)" : "(in time)", Math.round(r.getHurdleProbability() * 100))
                        + "\nvia " + r.getPathNodes().size() + " stops");
                setWrapText(true);
            }
        });
        listView.getSelectionModel().select(0);

        Button selectButton = new Button("Walk This Route");
        selectButton.getStyleClass().add("primary-button");
        Button cancelButton = new Button("Cancel");

        final Route[] selected = new Route[1];
        selectButton.setOnAction(e -> {
            selected[0] = listView.getSelectionModel().getSelectedItem();
            stage.close();
        });
        cancelButton.setOnAction(e -> stage.close());

        HBox buttons = new HBox(10, selectButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, windowLabel, listView, buttons);

        Scene scene = new Scene(root, 560, 420);
        scene.getStylesheets().add(RouteSelectionDialog.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
        return selected[0];
    }
}
