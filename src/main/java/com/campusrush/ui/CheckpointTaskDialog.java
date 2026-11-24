package com.campusrush.ui;

import com.campusrush.challenge.CheckpointTask;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Interactive Keyboard Challenge Dialog for checkpoint-to-checkpoint transit.
 * Captures real-time keyboard events for sprints, spacebar bursts, PIN entry, and reflexes.
 */
public class CheckpointTaskDialog {

    public static class TaskResult {
        public final boolean passed;
        public final int timePenaltyMinutes;
        public final int bonusScore;

        public TaskResult(boolean passed, int timePenaltyMinutes, int bonusScore) {
            this.passed = passed;
            this.timePenaltyMinutes = timePenaltyMinutes;
            this.bonusScore = bonusScore;
        }
    }

    public static TaskResult showTask(Stage owner, CheckpointTask task) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(task.getTitle());

        VBox root = new VBox(16);
        root.getStyleClass().add("glass-panel");
        root.setStyle("-fx-background-color: rgba(18, 20, 32, 0.96); -fx-border-color: #00f2fe; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(520);

        Label titleLabel = new Label(task.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        Label descLabel = new Label(task.getDescription());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0e0f0; -fx-wrap-text: true; -fx-text-alignment: center;");
        descLabel.setWrapText(true);

        ProgressBar timerBar = new ProgressBar(1.0);
        timerBar.setPrefWidth(440);
        timerBar.setStyle("-fx-accent: #00f2fe;");

        Label statusLabel = new Label("Use your KEYBOARD to complete the task!");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        final TaskResult[] finalResult = new TaskResult[1];
        final boolean[] taskFinished = {false};

        // Middle Interactive Area based on Task Type
        Pane taskContent = new VBox(12);
        ((VBox) taskContent).setAlignment(Pos.CENTER);

        // Timeline Countdown
        final double totalSeconds = task.getTimeLimitSeconds();
        final double[] remaining = {totalSeconds};
        Timeline timer = new Timeline();

        Runnable onTaskPass = () -> {
            if (taskFinished[0]) return;
            taskFinished[0] = true;
            timer.stop();
            finalResult[0] = new TaskResult(true, 0, task.getBonusScore());
            statusLabel.setText("✨ SUCCESS! Sprint Cleared (+ " + task.getBonusScore() + " bonus pts, 0 delay)");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00e676;");
            closeAfterDelay(stage, 750);
        };

        Runnable onTaskFail = () -> {
            if (taskFinished[0]) return;
            taskFinished[0] = true;
            timer.stop();
            finalResult[0] = new TaskResult(false, task.getTimePenaltyMinutes(), 0);
            statusLabel.setText("⚠️ FAILED! Checkpoint Delay: +" + task.getTimePenaltyMinutes() + " mins penalty");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ff416c;");
            closeAfterDelay(stage, 900);
        };

        // Build specific UI & Key Event Handlers for each task type
        switch (task.getType()) {
            case KEY_COMBO:
                buildKeyComboUI((VBox) taskContent, task.getTargetData(), root, onTaskPass, onTaskFail);
                break;
            case RAPID_SPACEBAR:
                buildRapidSpacebarUI((VBox) taskContent, root, onTaskPass);
                break;
            case GATE_PIN:
                buildGatePinUI((VBox) taskContent, task.getTargetData(), root, onTaskPass, onTaskFail);
                break;
            case CAMPUS_MATH:
                buildCampusMathUI((VBox) taskContent, task.getTargetData(), root, onTaskPass, onTaskFail);
                break;
            case GREEN_LIGHT:
                buildGreenLightUI((VBox) taskContent, root, onTaskPass, onTaskFail);
                break;
        }

        timer.getKeyFrames().add(new KeyFrame(Duration.millis(100), e -> {
            remaining[0] -= 0.1;
            double progress = Math.max(0, remaining[0] / totalSeconds);
            timerBar.setProgress(progress);
            if (progress < 0.3) {
                timerBar.setStyle("-fx-accent: #ff416c;");
            }
            if (remaining[0] <= 0 && !taskFinished[0]) {
                onTaskFail.run();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        root.getChildren().addAll(titleLabel, descLabel, taskContent, timerBar, statusLabel);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Make root focusable immediately
        stage.setOnShown(e -> {
            root.requestFocus();
        });

        stage.showAndWait();

        return finalResult[0] != null ? finalResult[0] : new TaskResult(false, task.getTimePenaltyMinutes(), 0);
    }

    private static void closeAfterDelay(Stage stage, int millis) {
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(millis), e -> stage.close()));
        delay.play();
    }

    // 1. KEY COMBO DASH
    private static void buildKeyComboUI(VBox box, String sequence, VBox root, Runnable onPass, Runnable onFail) {
        HBox keysBox = new HBox(12);
        keysBox.setAlignment(Pos.CENTER);

        List<Label> keyLabels = new ArrayList<>();
        for (char c : sequence.toCharArray()) {
            Label lbl = new Label(String.valueOf(c));
            lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-background-color: #24263a; -fx-padding: 8 16 8 16; -fx-background-radius: 8; -fx-border-color: #00f2fe; -fx-border-radius: 8;");
            keyLabels.add(lbl);
            keysBox.getChildren().add(lbl);
        }

        box.getChildren().add(keysBox);

        final int[] currentIndex = {0};

        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (currentIndex[0] >= sequence.length()) return;

            String pressed = event.getText().toUpperCase();
            String expected = String.valueOf(sequence.charAt(currentIndex[0])).toUpperCase();

            if (pressed.equals(expected)) {
                Label currentLabel = keyLabels.get(currentIndex[0]);
                currentLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-background-color: #00c853; -fx-padding: 8 16 8 16; -fx-background-radius: 8; -fx-border-color: #b9f6ca; -fx-border-radius: 8;");

                ScaleTransition st = new ScaleTransition(Duration.millis(120), currentLabel);
                st.setFromX(1.3);
                st.setFromY(1.3);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();

                currentIndex[0]++;
                if (currentIndex[0] == sequence.length()) {
                    onPass.run();
                }
            } else if (!pressed.isEmpty() && "WASD".contains(pressed)) {
                // Wrong key stumble
                Label currentLabel = keyLabels.get(currentIndex[0]);
                currentLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-background-color: #d50000; -fx-padding: 8 16 8 16; -fx-background-radius: 8; -fx-border-color: #ff8a80; -fx-border-radius: 8;");
            }
            event.consume();
        });
    }

    // 2. RAPID SPACEBAR MASH
    private static void buildRapidSpacebarUI(VBox box, VBox root, Runnable onPass) {
        ProgressBar sprintBar = new ProgressBar(0.0);
        sprintBar.setPrefWidth(380);
        sprintBar.setPrefHeight(26);
        sprintBar.setStyle("-fx-accent: #00e676;");

        Label countLabel = new Label("Taps: 0 / 10");
        countLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        box.getChildren().addAll(sprintBar, countLabel);

        final int[] tapCount = {0};
        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                tapCount[0]++;
                countLabel.setText("Taps: " + tapCount[0] + " / 10");
                sprintBar.setProgress(Math.min(1.0, tapCount[0] / 10.0));

                if (tapCount[0] >= 10) {
                    onPass.run();
                }
                event.consume();
            }
        });
    }

    // 3. GATE PIN RECALL
    private static void buildGatePinUI(VBox box, String pin, VBox root, Runnable onPass, Runnable onFail) {
        Label pinDisplay = new Label(pin);
        pinDisplay.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-background-color: #1f2238; -fx-padding: 8 20 8 20; -fx-background-radius: 8; -fx-border-color: #ffd700; -fx-border-radius: 8;");

        TextField inputField = new TextField();
        inputField.setPromptText("Enter PIN here...");
        inputField.setMaxWidth(200);
        inputField.setAlignment(Pos.CENTER);
        inputField.setStyle("-fx-font-size: 16px;");

        box.getChildren().addAll(pinDisplay, inputField);

        // Flash PIN for 1.4 seconds then mask with dots
        Timeline maskTimer = new Timeline(new KeyFrame(Duration.millis(1400), e -> {
            pinDisplay.setText("• • • •");
            inputField.requestFocus();
        }));
        maskTimer.play();

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals(pin)) {
                onPass.run();
            } else if (newVal.length() >= pin.length()) {
                onFail.run();
            }
        });
    }

    // 4. FAST CAMPUS MATH
    private static void buildCampusMathUI(VBox box, String correctAnswer, VBox root, Runnable onPass, Runnable onFail) {
        int correct = Integer.parseInt(correctAnswer);
        Random rand = new Random();

        int wrong1 = correct + (rand.nextBoolean() ? 2 : -2);
        int wrong2 = correct + (rand.nextBoolean() ? 5 : -5);

        List<Integer> choices = new ArrayList<>(List.of(correct, wrong1, wrong2));
        java.util.Collections.shuffle(choices);

        HBox optionsBox = new HBox(12);
        optionsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < choices.size(); i++) {
            int val = choices.get(i);
            char keyChar = (char) ('1' + i);
            Button btn = new Button("[" + keyChar + "]  " + val);
            btn.getStyleClass().add("primary-button");
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 8 16 8 16;");

            btn.setOnAction(e -> {
                if (val == correct) onPass.run();
                else onFail.run();
            });

            optionsBox.getChildren().add(btn);
        }

        box.getChildren().add(optionsBox);

        // Allow number keys 1, 2, 3
        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DIGIT1 || event.getCode() == KeyCode.NUMPAD1) {
                if (choices.get(0) == correct) onPass.run(); else onFail.run();
            } else if (event.getCode() == KeyCode.DIGIT2 || event.getCode() == KeyCode.NUMPAD2) {
                if (choices.size() > 1 && choices.get(1) == correct) onPass.run(); else onFail.run();
            } else if (event.getCode() == KeyCode.DIGIT3 || event.getCode() == KeyCode.NUMPAD3) {
                if (choices.size() > 2 && choices.get(2) == correct) onPass.run(); else onFail.run();
            }
        });
    }

    // 5. GREEN LIGHT REFLEX
    private static void buildGreenLightUI(VBox box, VBox root, Runnable onPass, Runnable onFail) {
        Circle light = new Circle(32, Color.RED);
        light.setStroke(Color.WHITE);
        light.setStrokeWidth(2.5);
        light.setEffect(new DropShadow(15, Color.RED));

        Label prompt = new Label("WAIT FOR GREEN LIGHT... (Do NOT tap yet!)");
        prompt.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ff5252;");

        box.getChildren().addAll(light, prompt);

        final boolean[] isGreen = {false};
        final boolean[] tooEarly = {false};

        // Turn green after random 1.2 to 2.5 seconds
        double delayMs = 1200 + new Random().nextInt(1300);
        Timeline greenTrigger = new Timeline(new KeyFrame(Duration.millis(delayMs), e -> {
            if (!tooEarly[0]) {
                isGreen[0] = true;
                light.setFill(Color.web("#00e676"));
                light.setEffect(new DropShadow(20, Color.web("#00e676")));
                prompt.setText("PRESS [SPACE] NOW!!");
                prompt.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00e676;");
            }
        }));
        greenTrigger.play();

        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (isGreen[0]) {
                    onPass.run();
                } else {
                    tooEarly[0] = true;
                    prompt.setText("False start! You tapped too early!");
                    onFail.run();
                }
                event.consume();
            }
        });
    }
}
