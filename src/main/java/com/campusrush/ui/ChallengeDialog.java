package com.campusrush.ui;

import com.campusrush.challenge.Challenge;
import com.campusrush.core.GameConfig;
import com.campusrush.hurdle.Hurdle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Interactive Challenge Dialog popping up when a hurdle is encountered.
 */
public class ChallengeDialog {

    public static class ChallengeResult {
        public boolean passed;
        public int timePenalty;

        public ChallengeResult(boolean passed, int timePenalty) {
            this.passed = passed;
            this.timePenalty = timePenalty;
        }
    }

    public static ChallengeResult showDialog(Stage owner, Hurdle hurdle) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("HURDLE ENCOUNTERED: " + hurdle.getName());

        VBox root = new VBox(15);
        root.getStyleClass().add("glass-panel");
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label headerLabel = new Label("⚠️ " + hurdle.getName());
        headerLabel.getStyleClass().add("section-header");
        headerLabel.setStyle("-fx-text-fill: #ff416c;");

        Label descLabel = new Label(hurdle.getDescription());
        descLabel.setWrapText(true);

        Challenge challenge = hurdle.getChallenge();
        Label qLabel = new Label(challenge.getQuestionText());
        qLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #00f2fe;");
        qLabel.setWrapText(true);

        VBox optionsBox = new VBox(8);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup group = new ToggleGroup();
        RadioButton[] radios = new RadioButton[challenge.getOptions().size()];
        for (int i = 0; i < challenge.getOptions().size(); i++) {
            RadioButton rb = new RadioButton(challenge.getOptions().get(i));
            rb.setToggleGroup(group);
            rb.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            rb.setUserData(i + 1);
            radios[i] = rb;
            optionsBox.getChildren().add(rb);
        }
        if (radios.length > 0) radios[0].setSelected(true);

        ProgressBar timerBar = new ProgressBar(1.0);
        timerBar.setPrefWidth(400);
        timerBar.setStyle("-fx-accent: #ff416c;");

        Button submitBtn = new Button("Submit Answer");
        submitBtn.getStyleClass().add("primary-button");

        final ChallengeResult[] result = new ChallengeResult[1];

        final double total = GameConfig.QUIZ_SECONDS;
        final double[] timeLeft = {total};
        Timeline countdown = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            timeLeft[0] -= 0.1;
            timerBar.setProgress(Math.max(0, timeLeft[0] / total));
            if (timeLeft[0] <= 0) {
                result[0] = new ChallengeResult(false, hurdle.getTimeDelayMinutes());
                Alert alert = new Alert(Alert.AlertType.WARNING, "Time expired! Hurdle delay applied (+ " + result[0].timePenalty + " mins)");
                alert.showAndWait();
                stage.close();
            }
        }));
        countdown.setCycleCount((int) (GameConfig.QUIZ_SECONDS * 10));
        countdown.play();

        submitBtn.setOnAction(e -> {
            countdown.stop();
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            String ansText = selected != null ? selected.getText() : "";

            if (challenge.validateAnswer(ansText)) {
                result[0] = new ChallengeResult(true, 0);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Correct! You push through with no delay.");
                alert.showAndWait();
            } else {
                result[0] = new ChallengeResult(false, hurdle.getTimeDelayMinutes());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Incorrect! Correct answer was: " + challenge.getCorrectAnswerDisplay() + "\nPenalty: +" + result[0].timePenalty + " mins delay.");
                alert.showAndWait();
            }
            stage.close();
        });

        root.getChildren().addAll(headerLabel, descLabel, qLabel, optionsBox, timerBar, submitBtn);

        Scene scene = new Scene(root, 480, 420);
        scene.getStylesheets().add(ChallengeDialog.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();

        return result[0] != null ? result[0] : new ChallengeResult(false, hurdle.getTimeDelayMinutes());
    }
}
