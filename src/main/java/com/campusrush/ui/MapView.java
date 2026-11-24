package com.campusrush.ui;

import com.campusrush.ability.Ability;
import com.campusrush.challenge.Challenge;
import com.campusrush.challenge.CheckpointTask;
import com.campusrush.core.*;
import com.campusrush.core.exceptions.CampusRushException;
import com.campusrush.hurdle.*;
import com.campusrush.map.*;
import com.campusrush.mission.Mission;
import com.campusrush.mission.MissionState;
import com.campusrush.player.Player;
import com.campusrush.util.AppLogger;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Main Interactive Map View.
 * Features:
 * - Authentic campus map blueprint background layout
 * - Real-time Keyboard-driven Movement (WASD / Arrow Keys / 1-4) between checkpoints
 * - Interactive Checkpoint Transit Tasks (Key Combos, Spacebar Stamina Rush, Turnstile PINs, Math)
 * - Animated player token movement along roads
 * - Active mission quest beacons & real-time path illuminating
 * - Interactive Map Viewport with Zoom & Pan controls
 */
public class MapView {

    private static final double MAP_WIDTH = 1280.0;
    private static final double MAP_HEIGHT = 709.0;
    private static final Color[] TOKEN_COLORS = {
            Color.web("#00f2fe"), // Player 1 - Cyan
            Color.web("#ff416c"), // Player 2 - Crimson
            Color.web("#ffd700"), // Player 3 - Gold
            Color.web("#00e676")  // Player 4 - Emerald
    };

    private final MainApp app;
    private final GameEngine engine;

    // Viewport & Map Layers
    private BorderPane mainRoot;
    private ScrollPane mapScrollPane;
    private Pane mapContentPane;
    private Group zoomGroup;
    private Pane edgesLayer;
    private Pane routeHighlightLayer;
    private Pane navBadgesLayer;
    private Pane questBeaconsLayer;
    private Pane nodesLayer;
    private Pane playersLayer;

    private double currentZoomScale = 1.0;
    private boolean labelsVisible = false;   // false = smart labels (only the places that matter now)
    private Rectangle tintOverlay;
    private boolean isMoving = false;
    private Route guideRoute;
    private Mission guideMission;

    // HUD Elements
    private Label clockLabel;
    private Label turnLabel;
    private Label phaseLabel;
    private ListView<Mission> missionListView;
    private VBox playerStatsBox;
    private Label routePreviewLabel;
    private VBox legendBox;
    private VBox keyboardNavBox;

    // Entity Tracking
    private final Map<String, StackPane> nodeWaypointViews = new HashMap<>();
    private final Map<String, Label> nodeLabels = new HashMap<>();
    private final Map<Player, StackPane> playerTokenViews = new HashMap<>();
    private final List<Animation> activeAnimations = new ArrayList<>();

    public MapView(MainApp app, GameEngine engine) {
        this.app = app;
        this.engine = engine;
    }

    public Parent getView() {
        mainRoot = new BorderPane();
        mainRoot.setStyle("-fx-background-color: #0b0c13;");

        // Top HUD Bar
        HBox topBar = createTopBar();
        mainRoot.setTop(topBar);

        // Center Campus Map Area (Interactive with Zoom/Pan & Overlays)
        StackPane centerMapStack = createCenterMapArea();
        mainRoot.setCenter(centerMapStack);

        // Right Side HUD
        VBox rightSide = createRightSideHUD();
        mainRoot.setRight(rightSide);

        // The clock is simulated: it only moves when a player acts, so the HUD is refreshed after each action.

        // Listen for WASD / Arrow / Number keys for direct player movement
        setupKeyboardControls();

        // Initial render & fit
        renderCampusMapGraph();
        refreshActiveMissionBeacons();
        refreshNavBadges();
        updateMovementHUD();
        updateClockUI();
        applyLabelVisibility();

        // Fit the map as soon as the viewport has a real size (it has none before the scene is shown).
        final boolean[] fitted = {false};
        mapScrollPane.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
            if (!fitted[0] && newB != null && newB.getWidth() > 200 && newB.getHeight() > 200) {
                fitted[0] = true;
                javafx.application.Platform.runLater(this::fitMapToView);
            }
        });
        javafx.application.Platform.runLater(() -> {
            fitMapToView();
            mainRoot.requestFocus();
        });

        return mainRoot;
    }

    private void setupKeyboardControls() {
        mainRoot.setFocusTraversable(true);
        mainRoot.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (isMovementKey(code)) {
                handleKeyboardMovement(code);
                event.consume();
            }
        });
    }

    private boolean isMovementKey(KeyCode code) {
        return code == KeyCode.W || code == KeyCode.A || code == KeyCode.S || code == KeyCode.D ||
               code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT ||
               code == KeyCode.DIGIT1 || code == KeyCode.DIGIT2 || code == KeyCode.DIGIT3 || code == KeyCode.DIGIT4 ||
               code == KeyCode.NUMPAD1 || code == KeyCode.NUMPAD2 || code == KeyCode.NUMPAD3 || code == KeyCode.NUMPAD4;
    }

    private HBox createTopBar() {
        HBox top = new HBox(20);
        top.setPadding(new Insets(12, 22, 12, 22));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("glass-panel");

        Label title = new Label("CAMPUS RUSH");
        title.getStyleClass().add("title-label");
        title.setStyle("-fx-font-size: 22px;");

        clockLabel = new Label("Time: --:--");
        clockLabel.getStyleClass().add("hud-value");

        turnLabel = new Label("Current Turn: " + engine.getCurrentPlayer().getName());
        turnLabel.getStyleClass().add("hud-label");

        phaseLabel = new Label("");
        phaseLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        Button waitBtn = new Button("⏳ Wait " + GameConfig.WAIT_MINUTES + "m");
        waitBtn.setTooltip(new Tooltip("Stand still and let game time pass (e.g. until a mission window opens)"));
        waitBtn.setOnAction(e -> {
            if (isMoving) return;
            afterStep(engine.waitMinutes(0));
        });

        Button abilityBtn = new Button("✨ Ability");
        abilityBtn.getStyleClass().add("accent-button");
        abilityBtn.setOnAction(e -> useSpecialAbility());

        Button saveBtn = new Button("💾 Save");
        saveBtn.setOnAction(e -> saveGameToFile());

        Button endBtn = new Button("🏁 Finish");
        endBtn.setTooltip(new Tooltip("Stop playing: this player keeps their score and forfeits the remaining time"));
        endBtn.setOnAction(e -> {
            if (isMoving) return;
            engine.finishCurrentPlayer();
            afterStep(new StepResult());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        top.getChildren().addAll(title, clockLabel, phaseLabel, turnLabel, waitBtn, spacer, abilityBtn, saveBtn, endBtn);
        return top;
    }

    private StackPane createCenterMapArea() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #0c0d16;");

        // Map Content Container: Image + Layers
        mapContentPane = new Pane();
        mapContentPane.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        mapContentPane.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        mapContentPane.setMaxSize(MAP_WIDTH, MAP_HEIGHT);

        // Load Authentic Campus Map Image
        ImageView mapImageView = loadMapImageView();
        mapContentPane.getChildren().add(mapImageView);

        // Time-of-day tint over the (dimmed) map: warm afternoon, orange dusk, deep-blue night.
        tintOverlay = new Rectangle(MAP_WIDTH, MAP_HEIGHT);
        tintOverlay.setMouseTransparent(true);
        mapContentPane.getChildren().add(tintOverlay);

        // Visual Layers on top of the campus map
        edgesLayer = new Pane();
        routeHighlightLayer = new Pane();
        navBadgesLayer = new Pane();
        questBeaconsLayer = new Pane();
        nodesLayer = new Pane();
        playersLayer = new Pane();

        mapContentPane.getChildren().addAll(
                edgesLayer,
                routeHighlightLayer,
                navBadgesLayer,
                questBeaconsLayer,
                nodesLayer,
                playersLayer
        );

        // Wrap in Zoom Group for seamless ScrollPane zoom
        zoomGroup = new Group(mapContentPane);

        mapScrollPane = new ScrollPane();
        mapScrollPane.setContent(zoomGroup);
        mapScrollPane.setPannable(true);
        mapScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mapScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mapScrollPane.getStyleClass().add("map-viewport-scroll");

        // Keep keyboard focus active when clicking anywhere on map
        mapScrollPane.setOnMouseClicked(e -> mainRoot.requestFocus());

        // Mouse wheel zooming
        mapScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                zoomBy(0.12);
            } else if (event.getDeltaY() < 0) {
                zoomBy(-0.12);
            }
            event.consume();
        });

        // Floating Map Controls Toolbar (Top-Left)
        HBox mapControls = createMapControlsToolbar();
        StackPane.setAlignment(mapControls, Pos.TOP_LEFT);
        StackPane.setMargin(mapControls, new Insets(15, 0, 0, 15));

        // Floating Legend Box (Bottom-Left)
        legendBox = createMapLegendBox();
        legendBox.setVisible(false);   // opened with the Legend button so it never hides the map
        legendBox.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(legendBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(legendBox, new Insets(0, 0, 15, 15));

        container.getChildren().addAll(mapScrollPane, mapControls, legendBox);
        return container;
    }

    private ImageView loadMapImageView() {
        ImageView iv = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream("/campus_map.jpg");
            if (is == null) {
                is = getClass().getResourceAsStream("/map.jpg");
            }
            if (is != null) {
                Image img = new Image(is);
                iv.setImage(img);
                iv.setFitWidth(MAP_WIDTH);
                iv.setFitHeight(MAP_HEIGHT);
                iv.setSmooth(true);
                // Mute the busy photo so roads, pins and labels stand out clearly.
                ColorAdjust dim = new ColorAdjust();
                dim.setSaturation(-0.55);
                dim.setBrightness(-0.42);
                dim.setContrast(0.12);
                iv.setEffect(dim);
                AppLogger.info("Loaded campus map image (" + (int) img.getWidth() + "x" + (int) img.getHeight() + ")");
            } else {
                AppLogger.warning("Campus map image file not found in resources!");
            }
        } catch (Exception ex) {
            AppLogger.severe("Failed to load map image: " + ex.getMessage(), ex);
        }
        return iv;
    }

    private HBox createMapControlsToolbar() {
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button zoomInBtn = new Button("＋");
        zoomInBtn.getStyleClass().add("map-control-btn");
        zoomInBtn.setTooltip(new Tooltip("Zoom In"));
        zoomInBtn.setOnAction(e -> { zoomBy(0.15); mainRoot.requestFocus(); });

        Button zoomOutBtn = new Button("－");
        zoomOutBtn.getStyleClass().add("map-control-btn");
        zoomOutBtn.setTooltip(new Tooltip("Zoom Out"));
        zoomOutBtn.setOnAction(e -> { zoomBy(-0.15); mainRoot.requestFocus(); });

        Button resetBtn = new Button("100%");
        resetBtn.getStyleClass().add("map-control-btn");
        resetBtn.setTooltip(new Tooltip("Reset Zoom (100%)"));
        resetBtn.setOnAction(e -> { setZoom(1.0); mainRoot.requestFocus(); });

        Button fitBtn = new Button("⛶ Fit");
        fitBtn.getStyleClass().add("map-control-btn");
        fitBtn.setTooltip(new Tooltip("Fit Map to Screen"));
        fitBtn.setOnAction(e -> { fitMapToView(); mainRoot.requestFocus(); });

        Button labelsBtn = new Button("🏷 All Labels");
        labelsBtn.getStyleClass().add("map-control-btn");
        labelsBtn.setTooltip(new Tooltip("Toggle Location Labels"));
        labelsBtn.setOnAction(e -> { toggleLabels(); mainRoot.requestFocus(); });

        Button legendBtn = new Button("🗺 Legend");
        legendBtn.getStyleClass().add("map-control-btn");
        legendBtn.setTooltip(new Tooltip("Toggle Map Legend"));
        legendBtn.setOnAction(e -> { legendBox.setVisible(!legendBox.isVisible()); mainRoot.requestFocus(); });

        toolbar.getChildren().addAll(zoomInBtn, zoomOutBtn, resetBtn, fitBtn, labelsBtn, legendBtn);
        return toolbar;
    }

    private VBox createMapLegendBox() {
        VBox box = new VBox(6);
        box.getStyleClass().add("map-legend-box");
        box.setMaxWidth(240);

        Label header = new Label("Campus Map Legend");
        header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        box.getChildren().add(header);
        box.getChildren().add(createLegendItem(Color.web("#00e5ff"), "Academic & Departments"));
        box.getChildren().add(createLegendItem(Color.web("#ff6b9d"), "Hostels & Residences"));
        box.getChildren().add(createLegendItem(Color.web("#00e676"), "Sports & Outdoor Arenas"));
        box.getChildren().add(createLegendItem(Color.web("#ffab00"), "Dining & Food Courts"));
        box.getChildren().add(createLegendItem(Color.web("#ff3d71"), "Campus Entrances / Gates"));
        box.getChildren().add(createLegendItem(Color.web("#ffd700"), "Active Mission Targets ⭐"));
        box.getChildren().add(createLegendItem(Color.web("#5a5a6e"), "Closed right now 🔒"));
        box.getChildren().add(createLegendItem(Color.web("#bfeaff"), "Road (solid)"));
        box.getChildren().add(createLegendItem(Color.web("#ff9f1c"), "Shortcut (dashed, risky)"));
        box.getChildren().add(createLegendItem(Color.web("#00f2fe"), "Dashed ring = you can walk here"));

        return box;
    }

    private HBox createLegendItem(Color color, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5, color);
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(1.0);

        Label lbl = new Label(text);
        lbl.getStyleClass().add("map-legend-item");

        row.getChildren().addAll(dot, lbl);
        return row;
    }

    private void zoomBy(double factor) {
        setZoom(currentZoomScale + factor);
    }

    private void setZoom(double scale) {
        currentZoomScale = Math.max(0.45, Math.min(2.5, scale));
        mapContentPane.setScaleX(currentZoomScale);
        mapContentPane.setScaleY(currentZoomScale);
    }

    private void fitMapToView() {
        if (mapScrollPane == null || mapScrollPane.getViewportBounds() == null) return;
        double viewportW = mapScrollPane.getViewportBounds().getWidth();
        double viewportH = mapScrollPane.getViewportBounds().getHeight();
        if (viewportW > 50 && viewportH > 50) {
            double scaleX = viewportW / MAP_WIDTH;
            double scaleY = viewportH / MAP_HEIGHT;
            double fitScale = Math.min(scaleX, scaleY) * 0.96;
            setZoom(fitScale);
        }
    }

    private void toggleLabels() {
        labelsVisible = !labelsVisible;
        applyLabelVisibility();
    }

    /** Smart labels: only your position, reachable neighbours, mission targets and the guide route (or all, if toggled). */
    private void applyLabelVisibility() {
        Set<String> important = new HashSet<>();
        Player p = engine.getCurrentPlayer();
        important.add(p.getCurrentLocation().getId());
        for (RouteEdge e : engine.getCampusMap().getEdgesFrom(p.getCurrentLocation())) important.add(e.getToLocation().getId());
        for (Mission m : engine.getMissionManager().getOpenMissions(p)) important.add(m.getDestination().getId());
        if (guideRoute != null) for (Location l : guideRoute.getPathNodes()) important.add(l.getId());
        for (Map.Entry<String, Label> en : nodeLabels.entrySet()) {
            en.getValue().setVisible(labelsVisible || important.contains(en.getKey()));
        }
    }

    private void tintForPhase() {
        if (tintOverlay == null) return;
        String name = engine.getCurrentPhase().getName();
        Color c;
        switch (name) {
            case "AFTERNOON": c = Color.rgb(255, 190, 80, 0.10); break;
            case "EVENING":   c = Color.rgb(255, 110, 40, 0.20); break;
            case "NIGHT":     c = Color.rgb(10, 20, 90, 0.42); break;
            default:          c = Color.TRANSPARENT;
        }
        tintOverlay.setFill(c);
    }

    private VBox createRightSideHUD() {
        VBox right = new VBox(14);
        right.setPrefWidth(330);
        right.setPadding(new Insets(15));
        right.getStyleClass().add("glass-panel");

        // 1. Mission Section
        Label missionHeader = new Label("Missions");
        missionHeader.getStyleClass().add("section-header");

        missionListView = new ListView<>();
        missionListView.setPrefHeight(190);
        missionListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Mission m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setText(null); setStyle(""); return; }
                MissionState st = engine.getMissionManager().stateFor(engine.getCurrentPlayer(), m);
                String tag = st == MissionState.LOCKED ? "🕒 opens " + m.getAvailableFrom().toLocalTime()
                        : "⏰ until " + m.getDeadline().toLocalTime();
                setText((m.isSpecial() ? "⭐ " : "") + m.getTitle() + "\n" + m.getDestination().getName()
                        + " · " + tag + " · " + m.getRewardPoints() + " pts");
                setWrapText(false);
                setStyle(st == MissionState.LOCKED ? "-fx-text-fill: #8a8aa8;" : "-fx-text-fill: #ffffff;");
            }
        });
        missionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            previewRouteForMission(newVal);
            refreshNavBadges();
        });
        updateMissionsList();

        routePreviewLabel = new Label("Phase: hover ☀ in the top bar for today's rules");
        routePreviewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #a0a0c0; -fx-wrap-text: true;");

        Button acceptMissionBtn = new Button("Show Route Guide for Mission");
        acceptMissionBtn.getStyleClass().add("primary-button");
        acceptMissionBtn.setMaxWidth(Double.MAX_VALUE);
        acceptMissionBtn.setOnAction(e -> {
            handleMissionSelection();
            mainRoot.requestFocus();
        });

        // 2. Keyboard Checkpoint Navigation Section
        Label navHeader = new Label("🎮 Keyboard Navigation");
        navHeader.getStyleClass().add("section-header");
        navHeader.setStyle("-fx-text-fill: #00f2fe;");

        keyboardNavBox = new VBox(6);

        // 3. Player Status Section
        Label statsHeader = new Label("Player Status");
        statsHeader.getStyleClass().add("section-header");

        playerStatsBox = new VBox(8);
        updatePlayerStatsUI();

        right.getChildren().addAll(
                missionHeader,
                missionListView,
                routePreviewLabel,
                acceptMissionBtn,
                navHeader,
                keyboardNavBox,
                statsHeader,
                playerStatsBox
        );
        return right;
    }

    /**
     * Renders campus graph nodes, roads, waypoints, and players on top of authentic map.
     */
    private void renderCampusMapGraph() {
        edgesLayer.getChildren().clear();
        routeHighlightLayer.getChildren().clear();
        nodesLayer.getChildren().clear();
        playersLayer.getChildren().clear();
        nodeWaypointViews.clear();
        nodeLabels.clear();
        playerTokenViews.clear();

        CampusMap map = engine.getCampusMap();
        Set<String> drawnEdges = new HashSet<>();

        // Draw Road Edges with semi-transparent subtle neon styling
        for (Location loc : map.getAllLocations()) {
            for (RouteEdge edge : map.getEdgesFrom(loc)) {
                String edgeKey = loc.getId().compareTo(edge.getToLocation().getId()) < 0
                        ? loc.getId() + "-" + edge.getToLocation().getId()
                        : edge.getToLocation().getId() + "-" + loc.getId();

                if (!drawnEdges.contains(edgeKey)) {
                    drawnEdges.add(edgeKey);
                    Line casing = new Line(loc.getX(), loc.getY(), edge.getToLocation().getX(), edge.getToLocation().getY());
                    casing.setStroke(Color.rgb(5, 8, 16, 0.75));
                    casing.setStrokeWidth(7.0);
                    casing.setStrokeLineCap(StrokeLineCap.ROUND);
                    casing.setMouseTransparent(true);

                    Line roadLine = new Line(loc.getX(), loc.getY(), edge.getToLocation().getX(), edge.getToLocation().getY());
                    roadLine.setStrokeLineCap(StrokeLineCap.ROUND);
                    if (edge.isShortcut()) {
                        roadLine.setStroke(Color.web("#ff9f1c"));
                        roadLine.setStrokeWidth(3.2);
                        roadLine.getStrokeDashArray().addAll(9.0, 7.0);
                    } else {
                        roadLine.setStroke(Color.rgb(190, 235, 255, 0.92));
                        roadLine.setStrokeWidth(3.2);
                    }
                    Tooltip.install(roadLine, new Tooltip((edge.isShortcut() ? "Shortcut: " : "Road: ")
                            + edge.getDistanceMinutes() + " min, hurdle risk " + Math.round(edge.getHurdleChance() * 100) + "%"));
                    edgesLayer.getChildren().addAll(casing, roadLine);
                }
            }
        }

        // Draw Location Nodes / Waypoints
        for (Location loc : map.getAllLocations()) {
            boolean closedNow = !engine.getSchedule().isOpen(loc, engine.getCurrentTime());
            Color catColor = closedNow ? Color.web("#5a5a6e") : getLocationCategoryColor(loc.getId());

            StackPane waypoint = new StackPane();
            waypoint.setLayoutX(loc.getX() - 10);
            waypoint.setLayoutY(loc.getY() - 10);
            waypoint.setPrefSize(20, 20);
            waypoint.setCursor(javafx.scene.Cursor.HAND);

            Circle glowCircle = new Circle(11, catColor.deriveColor(0, 1, 1, 0.30));
            Circle outerCircle = new Circle(8, catColor);
            outerCircle.setStroke(Color.WHITE);
            outerCircle.setStrokeWidth(1.6);
            Circle innerDot = new Circle(2.5, Color.WHITE);

            DropShadow pinShadow = new DropShadow(6, Color.BLACK);
            waypoint.setEffect(pinShadow);
            waypoint.getChildren().addAll(glowCircle, outerCircle, innerDot);
            if (isAdjacentNeighbor(engine.getCurrentPlayer().getCurrentLocation(), loc) && !closedNow) {
                Circle ring = new Circle(15, Color.TRANSPARENT);   // "you can walk here" ring
                ring.setStroke(Color.web("#00f2fe"));
                ring.setStrokeWidth(2.0);
                ring.getStrokeDashArray().addAll(4.0, 3.0);
                waypoint.getChildren().add(0, ring);
            }

            Label nameLabel = new Label((closedNow ? "🔒 " : "") + loc.getName());
            nameLabel.getStyleClass().add("map-node-label");
            nameLabel.setLayoutX(loc.getX() - 35);
            nameLabel.setLayoutY(loc.getY() + 10);
            nameLabel.setMouseTransparent(true);

            Tooltip tooltip = new Tooltip(
                    loc.getName() + "\n" +
                    "Category: " + getLocationCategoryName(loc.getId()) + (closedNow ? " (CLOSED right now)" : "") + "\n" +
                    "Neighbors: " + map.getEdgesFrom(loc).size() + " connected paths"
            );
            tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #1a1c2b; -fx-text-fill: #ffffff;");
            Tooltip.install(waypoint, tooltip);

            waypoint.setOnMouseEntered(e -> {
                nameLabel.setVisible(true);
                nameLabel.toFront();
                waypoint.setScaleX(1.35);
                waypoint.setScaleY(1.35);
                outerCircle.setStroke(Color.GOLD);
            });
            waypoint.setOnMouseExited(e -> {
                applyLabelVisibility();
                waypoint.setScaleX(1.0);
                waypoint.setScaleY(1.0);
                outerCircle.setStroke(Color.WHITE);
            });

            // Clicking node previews location details; movement must be done via keyboard keys
            waypoint.setOnMouseClicked(e -> {
                Location current = engine.getCurrentPlayer().getCurrentLocation();
                if (isAdjacentNeighbor(current, loc)) {
                    routePreviewLabel.setText("⌨️ Movement is done by keyboard keys! Press the key (WASD / Arrows) towards " + loc.getName());
                } else {
                    handleNodeClick(loc);
                }
                mainRoot.requestFocus();
            });

            nodeWaypointViews.put(loc.getId(), waypoint);
            nodeLabels.put(loc.getId(), nameLabel);

            nodesLayer.getChildren().addAll(waypoint, nameLabel);
        }

        // Draw Player Tokens
        List<Player> players = engine.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Color col = TOKEN_COLORS[i % TOKEN_COLORS.length];
            StackPane tokenView = createPlayerTokenView(p, col, i + 1);
            playerTokenViews.put(p, tokenView);
            playersLayer.getChildren().add(tokenView);
        }
    }

    private boolean isAdjacentNeighbor(Location from, Location to) {
        for (RouteEdge edge : engine.getCampusMap().getEdgesFrom(from)) {
            if (edge.getToLocation().equals(to)) return true;
        }
        return false;
    }

    private StackPane createPlayerTokenView(Player player, Color color, int playerNum) {
        StackPane token = new StackPane();
        Location loc = player.getCurrentLocation();
        token.setLayoutX(loc.getX());
        token.setLayoutY(loc.getY());

        Circle pulseRing = new Circle(18, Color.TRANSPARENT);
        pulseRing.setStroke(color);
        pulseRing.setStrokeWidth(2.0);

        Circle baseCircle = new Circle(12, color);
        baseCircle.setStroke(Color.WHITE);
        baseCircle.setStrokeWidth(2.2);

        Label initialLabel = new Label("P" + playerNum);
        initialLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label nameTag = new Label(player.getName());
        nameTag.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
        nameTag.setTranslateY(18);

        token.getChildren().addAll(pulseRing, baseCircle, initialLabel, nameTag);
        token.setEffect(new DropShadow(8, color));

        if (player.equals(engine.getCurrentPlayer())) {
            ScaleTransition st = new ScaleTransition(Duration.millis(900), pulseRing);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.7);
            st.setToY(1.7);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.play();
            activeAnimations.add(st);
        }

        return token;
    }

    private void refreshActiveMissionBeacons() {
        questBeaconsLayer.getChildren().clear();

        List<Mission> available = engine.getMissionManager().getOpenMissions(engine.getCurrentPlayer());
        Set<String> missionDestinations = new HashSet<>();

        for (Mission m : available) {
            Location dest = m.getDestination();
            if (dest == null || missionDestinations.contains(dest.getId())) continue;
            missionDestinations.add(dest.getId());

            StackPane beacon = new StackPane();
            beacon.setLayoutX(dest.getX() - 15);
            beacon.setLayoutY(dest.getY() - 15);
            beacon.setPrefSize(30, 30);
            beacon.setMouseTransparent(true);

            Circle pulse = new Circle(14, Color.TRANSPARENT);
            pulse.setStroke(Color.GOLD);
            pulse.setStrokeWidth(2.5);

            Label star = new Label("⭐");
            star.setStyle("-fx-font-size: 14px;");
            star.setTranslateY(-14);

            beacon.getChildren().addAll(pulse, star);
            questBeaconsLayer.getChildren().add(beacon);

            ScaleTransition st = new ScaleTransition(Duration.millis(1100), pulse);
            st.setFromX(0.8);
            st.setFromY(0.8);
            st.setToX(2.0);
            st.setToY(2.0);
            st.setCycleCount(Animation.INDEFINITE);

            FadeTransition ft = new FadeTransition(Duration.millis(1100), pulse);
            ft.setFromValue(0.9);
            ft.setToValue(0.0);
            ft.setCycleCount(Animation.INDEFINITE);

            ParallelTransition pt = new ParallelTransition(st, ft);
            pt.play();
            activeAnimations.add(pt);

            Label lbl = nodeLabels.get(dest.getId());
            if (lbl != null) {
                if (!lbl.getStyleClass().contains("map-node-label-quest")) lbl.getStyleClass().add("map-node-label-quest");
                lbl.setText("⭐ " + dest.getName() + "  ⏰ " + m.getDeadline().toLocalTime());
            }
        }
    }

    /**
     * Calculates available adjacent moves with directional keys (W/A/S/D / Arrows / Numbers).
     */
    private Map<KeyCode, Location> calculateAdjacentDirections() {
        Map<KeyCode, Location> dirMap = new HashMap<>();
        Player player = engine.getCurrentPlayer();
        Location current = player.getCurrentLocation();
        List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(current);

        for (RouteEdge edge : edges) {
            Location neighbor = edge.getToLocation();
            double dx = neighbor.getX() - current.getX();
            double dy = neighbor.getY() - current.getY();

            double angle = Math.toDegrees(Math.atan2(-dy, dx)); // 90 Up, -90 Down, 0 Right, 180 Left

            KeyCode code;
            if (angle >= 45 && angle < 135) {
                code = KeyCode.W;
            } else if (angle >= -45 && angle < 45) {
                code = KeyCode.D;
            } else if (angle >= -135 && angle < -45) {
                code = KeyCode.S;
            } else {
                code = KeyCode.A;
            }

            if (!dirMap.containsKey(code)) {
                dirMap.put(code, neighbor);
            } else {
                // Find empty slot
                for (KeyCode fallback : new KeyCode[]{KeyCode.W, KeyCode.D, KeyCode.S, KeyCode.A}) {
                    if (!dirMap.containsKey(fallback)) {
                        dirMap.put(fallback, neighbor);
                        break;
                    }
                }
            }
        }
        return dirMap;
    }

    /**
     * Displays on-map floating directional key badges around the player's current checkpoint.
     */
    private void refreshNavBadges() {
        navBadgesLayer.getChildren().clear();

        Player player = engine.getCurrentPlayer();
        Location current = player.getCurrentLocation();
        Map<KeyCode, Location> dirMap = calculateAdjacentDirections();
        List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(current);

        // Check if there is an active mission target on path
        Location missionNextStep = guideNextStep();
        Mission activeMission = missionListView.getSelectionModel().getSelectedItem();
        if (missionNextStep == null && activeMission != null && !activeMission.getDestination().equals(current)) {
            try {
                List<Route> routes = engine.getCampusMap().findRoutes(current, activeMission.getDestination());
                if (!routes.isEmpty() && routes.get(0).getPathNodes().size() > 1) {
                    missionNextStep = routes.get(0).getPathNodes().get(1);
                }
            } catch (Exception ignored) {}
        }

        for (Map.Entry<KeyCode, Location> entry : dirMap.entrySet()) {
            KeyCode key = entry.getKey();
            Location neighbor = entry.getValue();

            // Position badge roughly 38% along the road from player to neighbor
            double bx = current.getX() + (neighbor.getX() - current.getX()) * 0.38;
            double by = current.getY() + (neighbor.getY() - current.getY()) * 0.38;

            HBox badge = new HBox(4);
            badge.setAlignment(Pos.CENTER);
            badge.getStyleClass().add("nav-key-badge");
            badge.setLayoutX(bx - 30);
            badge.setLayoutY(by - 12);

            boolean isMissionRoute = neighbor.equals(missionNextStep);
            if (isMissionRoute) {
                badge.getStyleClass().add("nav-key-badge-mission");
            }

            Label keyLbl = new Label("[" + key.getName() + "]");
            keyLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #00f2fe;");

            Label destLbl = new Label((isMissionRoute ? "⭐ " : "") + neighbor.getName());
            destLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

            badge.getChildren().addAll(keyLbl, destLbl);

            badge.setOnMouseClicked(e -> {
                routePreviewLabel.setText("⌨️ Movement must be done by keyboard! Press key [" + key.getName() + "] to move to " + neighbor.getName());
                mainRoot.requestFocus();
            });

            navBadgesLayer.getChildren().add(badge);
        }
    }

    /**
     * Updates the on-screen Checkpoint Movement HUD showing keyboard keys.
     */
    private void updateMovementHUD() {
        if (keyboardNavBox == null) return;
        keyboardNavBox.getChildren().clear();

        Player player = engine.getCurrentPlayer();
        Location current = player.getCurrentLocation();
        Map<KeyCode, Location> dirMap = calculateAdjacentDirections();
        List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(current);

        Label currentLocLbl = new Label("📍 Current: " + current.getName());
        currentLocLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        Label movePrompt = new Label("⌨️ PRESS KEY ON KEYBOARD TO MOVE:");
        movePrompt.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        keyboardNavBox.getChildren().addAll(currentLocLbl, movePrompt);

        if (edges.isEmpty()) {
            Label noMove = new Label("No adjacent checkpoints available.");
            keyboardNavBox.getChildren().add(noMove);
            return;
        }

        int index = 1;
        for (RouteEdge edge : edges) {
            Location neighbor = edge.getToLocation();

            String keyStr = String.valueOf(index);
            for (Map.Entry<KeyCode, Location> e : dirMap.entrySet()) {
                if (e.getValue().equals(neighbor)) {
                    keyStr = e.getKey().getName();
                    break;
                }
            }

            boolean closedNb = !engine.getSchedule().isOpen(neighbor, engine.getCurrentTime());
            Button navBtn = new Button(String.format("⌨️ [%s] ➔ %s (%d min, risk %d%%)%s%s", keyStr, neighbor.getName(),
                    edge.getDistanceMinutes(), Math.round(edge.getHurdleChance() * 100),
                    edge.isShortcut() ? " ⚡shortcut" : "", closedNb ? " 🔒" : ""));
            navBtn.getStyleClass().add("nav-direction-btn");
            navBtn.setMaxWidth(Double.MAX_VALUE);

            final String keyPrompt = keyStr;
            final Location target = neighbor;
            navBtn.setOnAction(e -> {
                routePreviewLabel.setText("⌨️ Press key [" + keyPrompt + "] on your keyboard to move to " + target.getName());
                mainRoot.requestFocus();
            });

            keyboardNavBox.getChildren().add(navBtn);
            index++;
        }
    }

    public void handleKeyPressed(KeyCode code) {
        if (isMovementKey(code)) {
            handleKeyboardMovement(code);
        }
    }

    public void handleKeyboardMovement(KeyCode code) {
        if (isMoving) return;

        Map<KeyCode, Location> dirMap = calculateAdjacentDirections();
        Location target = null;

        if (code == KeyCode.W || code == KeyCode.UP) target = dirMap.get(KeyCode.W);
        else if (code == KeyCode.S || code == KeyCode.DOWN) target = dirMap.get(KeyCode.S);
        else if (code == KeyCode.A || code == KeyCode.LEFT) target = dirMap.get(KeyCode.A);
        else if (code == KeyCode.D || code == KeyCode.RIGHT) target = dirMap.get(KeyCode.D);
        else if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) target = getNeighborByIndex(0);
        else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) target = getNeighborByIndex(1);
        else if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) target = getNeighborByIndex(2);
        else if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) target = getNeighborByIndex(3);

        if (target != null) {
            startCheckpointTransit(target);
        } else {
            routePreviewLabel.setText("No connected road in direction [" + code.getName() + "]! Try another arrow/WASD key.");
        }
    }

    private Location getNeighborByIndex(int idx) {
        List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(engine.getCurrentPlayer().getCurrentLocation());
        if (idx >= 0 && idx < edges.size()) {
            return edges.get(idx).getToLocation();
        }
        return null;
    }

    /**
     * One step along a single road. The engine rolls whether a hurdle appears (risk depends on the
     * road, the time of day and the character). A failed hurdle costs time and the player stays put;
     * a solved one scores points and the player crosses.
     */
    private void startCheckpointTransit(Location target) {
        if (isMoving || engine.isGameOver()) return;
        isMoving = true;
        takeStep(target, () -> { }, true);
    }

    /** Runs one step; {@code onDone} runs after it finishes (moved or not). Returns via callback so routes can chain. */
    private void takeStep(Location target, Runnable onMoved, boolean standalone) {
        Player player = engine.getCurrentPlayer();
        StepPlan plan;
        try {
            plan = engine.planStep(target);
        } catch (CampusRushException ex) {
            isMoving = false;
            routePreviewLabel.setText("⛔ " + ex.getMessage());
            mainRoot.requestFocus();
            return;
        }

        HurdleOutcome outcome = null;
        if (plan.requiresChallenge()) {
            boolean passed;
            if (plan.getHurdle() != null) {
                passed = ChallengeDialog.showDialog(app.getPrimaryStage(), plan.getHurdle()).passed;
            } else {
                passed = CheckpointTaskDialog.showTask(app.getPrimaryStage(), plan.getReflexTask()).passed;
            }
            outcome = passed ? HurdleOutcome.PASSED : HurdleOutcome.FAILED;
        }

        if (outcome == HurdleOutcome.FAILED || !plan.isAffordable()) {
            StepResult res = engine.resolveStep(plan, outcome);
            playBlockedAnimation(playerTokenViews.get(player));
            isMoving = false;
            afterStep(res);
            return;
        }

        final HurdleOutcome finalOutcome = outcome;
        animatePlayerStep(player, plan.getFrom(), plan.getTo(), () -> {
            StepResult res = engine.resolveStep(plan, finalOutcome);
            isMoving = false;
            afterStep(res);
            if (res.moved() && !engine.isGameOver()) onMoved.run();
        });
    }

    /** Refreshes the HUD and tells the player what happened. */
    private void afterStep(StepResult res) {
        updateUIState();
        StringBuilder sb = new StringBuilder();
        for (String m : res.getMessages()) sb.append("• ").append(m).append("\n");
        if (res.completedAnyMission()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Mission Completed!");
            alert.setHeaderText("🎉 MISSION ACCOMPLISHED!");
            alert.setContentText(sb.toString());
            alert.show();
        } else if (res.blocked()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Blocked!");
            alert.setHeaderText("⛔ HURDLE FAILED — YOU STAY PUT");
            alert.setContentText(sb.toString());
            alert.show();
        } else if (sb.length() > 0) {
            routePreviewLabel.setText(sb.toString().trim());
        }
        if (engine.isGameOver()) {
            app.showLeaderboard(engine);
        }
        mainRoot.requestFocus();
    }

    private void playBlockedAnimation(StackPane token) {
        if (token == null) return;
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), token);
        shake.setFromX(0);
        shake.setByX(9);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> token.setTranslateX(0));
        shake.play();
    }

    private void animatePlayerStep(Player player, Location fromLoc, Location toLoc, Runnable onFinished) {
        StackPane token = playerTokenViews.get(player);
        if (token == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        double startX = fromLoc.getX();
        double startY = fromLoc.getY();
        double endX = toLoc.getX();
        double endY = toLoc.getY();

        token.setLayoutX(startX);
        token.setLayoutY(startY);
        token.setTranslateX(0);
        token.setTranslateY(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(420), token);
        tt.setFromX(0);
        tt.setFromY(0);
        tt.setToX(endX - startX);
        tt.setToY(endY - startY);

        tt.setOnFinished(e -> {
            token.setTranslateX(0);
            token.setTranslateY(0);
            token.setLayoutX(endX);
            token.setLayoutY(endY);
            if (onFinished != null) onFinished.run();
        });

        tt.play();
    }

    private void handleNodeClick(Location loc) {
        for (Mission m : missionListView.getItems()) {
            if (m.getDestination() != null && m.getDestination().getId().equals(loc.getId())) {
                missionListView.getSelectionModel().select(m);
                missionListView.scrollTo(m);
                previewRouteForMission(m);
                return;
            }
        }
        routePreviewLabel.setText("Selected: " + loc.getName() + " (" + getLocationCategoryName(loc.getId()) + ")");
    }

    private void previewRouteForMission(Mission mission) {
        routeHighlightLayer.getChildren().clear();
        if (mission == null) {
            routePreviewLabel.setText("Select a mission to preview the fastest route");
            return;
        }

        Player current = engine.getCurrentPlayer();
        Location start = current.getCurrentLocation();
        Location end = mission.getDestination();

        if (start.equals(end)) {
            routePreviewLabel.setText("You are already at " + end.getName() + " - window " + mission.windowText());
            return;
        }

        try {
            List<Route> routes = engine.findRoutes(end);
            Route bestRoute = routes.get(0);
            List<Location> nodes = bestRoute.getPathNodes();
            for (int i = 0; i < nodes.size() - 1; i++) {
                Location a = nodes.get(i);
                Location b = nodes.get(i + 1);
                Line hl = new Line(a.getX(), a.getY(), b.getX(), b.getY());
                hl.setStroke(Color.GOLD);
                hl.setStrokeWidth(4.5);
                hl.setStrokeLineCap(StrokeLineCap.ROUND);
                hl.setEffect(new DropShadow(10, Color.GOLD));
                routeHighlightLayer.getChildren().add(hl);
            }
            java.time.LocalDateTime eta = engine.getCurrentTime().plusSeconds(Math.round(bestRoute.getEstimatedMinutes() * 60));
            boolean inTime = !mission.isPastDeadline(eta);
            routePreviewLabel.setText(String.format("%s: ~%.0f min (ETA %s, window %s %s) | hurdle chance %d%%",
                    end.getName(), bestRoute.getEstimatedMinutes(), eta.toLocalTime().withSecond(0).withNano(0),
                    mission.windowText(), inTime ? "✅" : "❌ too late", Math.round(bestRoute.getHurdleProbability() * 100)));
        } catch (CampusRushException ex) {
            routePreviewLabel.setText("⛔ " + ex.getMessage());
        }
    }

    /**
     * Pick a mission and a route. The route is only a GUIDE drawn on the map: the token never
     * moves by itself - the player moves it one road at a time with WASD / arrows / 1-4.
     */
    private void handleMissionSelection() {
        if (isMoving || engine.isGameOver()) return;
        Mission selectedMission = missionListView.getSelectionModel().getSelectedItem();
        if (selectedMission == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a mission from the list!").show();
            return;
        }

        try {
            List<Route> candidateRoutes = engine.selectMissionAndFindRoutes(selectedMission);
            Route chosen = RouteSelectionDialog.showDialog(app.getPrimaryStage(), candidateRoutes,
                    selectedMission, engine.getCurrentTime());
            guideRoute = chosen;
            guideMission = chosen == null ? null : selectedMission;
            drawGuideRoute();
            refreshNavBadges();
            if (chosen != null) {
                routePreviewLabel.setText("🧭 Guide set: " + chosen.getLabel()
                        + ". Move yourself with the keyboard - follow the ⭐ key badge.");
            }
        } catch (CampusRushException ex) {
            AppLogger.warning("Mission selection error: " + ex.getMessage());
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
        }
        mainRoot.requestFocus();
    }

    /**
     * If the player stepped off the guide route, automatically suggest a new route to the same mission
     * (keeping their preferred style - fastest or safest - when possible). Clears the guide once the
     * mission is done, expired or reached.
     */
    private void refreshGuide() {
        if (guideMission == null) return;
        Player p = engine.getCurrentPlayer();
        MissionState st = engine.getMissionManager().stateFor(p, guideMission);
        boolean over = st == MissionState.COMPLETED || st == MissionState.EXPIRED
                || p.getCurrentLocation().equals(guideMission.getDestination());
        if (over) { guideMission = null; guideRoute = null; return; }
        if (guideRoute != null && guideRoute.getPathNodes().contains(p.getCurrentLocation())) return;   // still on route

        try {
            List<Route> options = engine.findRoutes(guideMission.getDestination());
            String preferred = guideRoute == null ? "Fastest" : guideRoute.getLabel().split(" ")[0];
            Route pick = options.get(0);
            for (Route r : options) {
                if (r.getLabel().startsWith(preferred)) { pick = r; break; }
            }
            guideRoute = pick;
            routePreviewLabel.setText("↩ You left the suggested route - new suggestion to "
                    + guideMission.getDestination().getName() + ": " + pick.getLabel()
                    + String.format(" (~%.0f min, hurdle chance %d%%)", pick.getEstimatedMinutes(),
                    Math.round(pick.getHurdleProbability() * 100)));
        } catch (CampusRushException ex) {
            guideRoute = null;
            routePreviewLabel.setText("⛔ No route to " + guideMission.getDestination().getName() + " right now: " + ex.getMessage());
        }
    }

    /** Draws the chosen guide route (only the part still ahead of the player). */
    private void drawGuideRoute() {
        routeHighlightLayer.getChildren().clear();
        if (guideRoute == null) return;
        List<Location> nodes = guideRoute.getPathNodes();
        int from = nodes.indexOf(engine.getCurrentPlayer().getCurrentLocation());
        if (from < 0) { guideRoute = null; return; }   // player left the route: guide no longer valid
        for (int i = from; i < nodes.size() - 1; i++) {
            Location a = nodes.get(i), b = nodes.get(i + 1);
            Line hl = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            hl.setStroke(Color.GOLD);
            hl.setStrokeWidth(4.5);
            hl.setStrokeLineCap(StrokeLineCap.ROUND);
            hl.setEffect(new DropShadow(10, Color.GOLD));
            routeHighlightLayer.getChildren().add(hl);
        }
    }

    /** Next stop of the guide route from the player's position, or null. */
    private Location guideNextStep() {
        if (guideRoute == null) return null;
        List<Location> nodes = guideRoute.getPathNodes();
        int i = nodes.indexOf(engine.getCurrentPlayer().getCurrentLocation());
        return (i >= 0 && i < nodes.size() - 1) ? nodes.get(i + 1) : null;
    }

    private void useSpecialAbility() {
        Player player = engine.getCurrentPlayer();
        try {
            Ability ability = engine.useAbility();
            new Alert(Alert.AlertType.INFORMATION, ability.getName() + " is armed!\n" + ability.getDescription()).show();
            updatePlayerStatsUI();
        } catch (CampusRushException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).show();
        }
        mainRoot.requestFocus();
    }

    private void saveGameToFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Game Session");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Campus Rush Save (*.sav)", "*.sav"));
        fileChooser.setInitialFileName("campusrush_save.sav");
        File file = fileChooser.showSaveDialog(app.getPrimaryStage());

        if (file != null) {
            try {
                engine.saveGameSession(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Game saved successfully to " + file.getName());
                alert.show();
            } catch (Exception ex) {
                AppLogger.severe("Failed to save game session", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save game: " + ex.getMessage());
                alert.show();
            }
        }
        mainRoot.requestFocus();
    }

    private void updateClockUI() {
        java.time.LocalDateTime t = engine.getCurrentTime();
        clockLabel.setText("🕒 " + t.toLocalTime().withSecond(0).withNano(0) + "  (" + engine.getRemainingMinutes() + "m left)");
        CampusSchedule.Phase ph = engine.getCurrentPhase();
        phaseLabel.setText("☀ " + ph.getName());
        phaseLabel.setTooltip(new Tooltip(ph.getDescription()));
        tintForPhase();
    }

    private void updateUIState() {
        if (engine.isGameOver()) return;
        turnLabel.setText("Turn: " + engine.getCurrentPlayer().getName());
        updateClockUI();
        updateMissionsList();
        updatePlayerStatsUI();
        renderCampusMapGraph();
        refreshActiveMissionBeacons();
        refreshGuide();
        drawGuideRoute();
        applyLabelVisibility();
        refreshNavBadges();
        updateMovementHUD();
        mainRoot.requestFocus();
    }

    private void updateMissionsList() {
        Mission selected = missionListView.getSelectionModel().getSelectedItem();
        List<Mission> visible = engine.getMissionManager().getVisibleMissions(engine.getCurrentPlayer(), 4);
        missionListView.getItems().setAll(visible);
        if (selected != null && visible.contains(selected)) missionListView.getSelectionModel().select(selected);
    }

    private void updatePlayerStatsUI() {
        playerStatsBox.getChildren().clear();
        for (Player p : engine.getPlayers()) {
            VBox card = new VBox(4);
            card.getStyleClass().add("card-panel");
            if (p.equals(engine.getCurrentPlayer()) && !p.isFinished()) {
                card.setStyle("-fx-border-color: #00f2fe; -fx-border-width: 2px; -fx-background-color: #1f2033;");
            }
            Label name = new Label(p.getName() + " (" + p.getCharacter().getName() + ")" + (p.isFinished() ? "  ✔ done" : ""));
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");

            Label loc = new Label("📍 " + p.getCurrentLocation().getName() + " | 🕒 "
                    + p.getClock().getCurrentGameTime().toLocalTime().withSecond(0).withNano(0));
            loc.setStyle("-fx-font-size: 11px; -fx-text-fill: #a0a0c0;");

            Label score = new Label("Score: " + p.getScore() + " | Missions: " + p.getCompletedMissionsCount()
                    + " | Charges: " + p.getAbilityCharges()
                    + (p.getArmedEffect() != null ? " (ARMED)" : ""));
            score.setStyle("-fx-font-size: 12px; -fx-text-fill: #00f2fe;");

            card.getChildren().addAll(name, loc, score);
            playerStatsBox.getChildren().add(card);
        }
    }

    public static Color getLocationCategoryColor(String id) {
        switch (LocationCategory.fromId(id)) {
            case GATE:     return Color.web("#ff3d71");
            case HOSTEL:   return Color.web("#ff6b9d");
            case OUTDOOR:  return Color.web("#00e676");
            case DINING:   return Color.web("#ffab00");
            default:       return Color.web("#00e5ff");
        }
    }

    public static String getLocationCategoryName(String id) {
        return LocationCategory.fromId(id).getDisplayName();
    }
}
