package td;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import td.exception.InsufficientGoldException;
import td.exception.InvalidPlacementException;
import td.model.Enemy;
import td.model.GameRecord;
import td.model.Tower;
import td.service.GameEngine;
import td.service.ReportGeneratorTask;
import td.service.WaveSpawnerTask;

import java.util.List;

/**
 * Main JavaFX Application for the Simple Tower Defense Game.
 * Features an integrated 3-tab layout: Battlefield, Armory Directory, and Match Analytics.
 */
public class TowerDefenseApp extends Application {

    private final GameEngine gameEngine = new GameEngine();

    // HUD Status Labels
    private final Label goldLabel = new Label();
    private final Label livesLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label threadStatusLabel = new Label("Thread Status: Idle");

    // Battlefield Controls
    private final Button[][] gridButtons = new Button[5][5];
    private final ComboBox<String> towerTypeCombo = new ComboBox<>();
    private final ToggleButton upgradeModeToggle = new ToggleButton("Mode: Build");
    private final ListView<String> notificationListView = new ListView<>();
    private final ObservableList<String> notificationItems = FXCollections.observableArrayList();

    // Armory Controls
    private final TableView<Tower> armoryTable = new TableView<>();
    private final ObservableList<Tower> armoryData = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();

    // Match History & Reports Controls
    private final TableView<GameRecord> historyTable = new TableView<>();
    private final ObservableList<GameRecord> historyData = FXCollections.observableArrayList();
    private final Label reportStatusLabel = new Label("Report Status: Idle");
    private final TextArea reportArea = new TextArea();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("OOP Tower Defense - Autonomous Strategy");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e2f; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Top HUD & Login Setup Bar
        VBox topContainer = createTopBar();
        root.setTop(topContainer);

        // Main Tab Navigation
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab battlefieldTab = new Tab("⚔ Battlefield", createBattlefieldView());
        Tab armoryTab = new Tab("🛡 Armory & Directory", createArmoryView());
        Tab reportsTab = new Tab("📊 Reports & History", createReportsView());

        tabPane.getTabs().addAll(battlefieldTab, armoryTab, reportsTab);
        root.setCenter(tabPane);

        // Initial GUI sync
        refreshHUD();
        refreshGrid();
        refreshArmoryTable();
        refreshHistoryTable();

        Scene scene = new Scene(root, 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    /**
     * Builds the top commander login bar and live HUD metrics.
     */
    private VBox createTopBar() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: #26263b; -fx-border-color: #3b3b54; -fx-border-width: 0 0 2 0;");

        // Row 1: Player Name & Difficulty Settings
        HBox setupRow = new HBox(12);
        setupRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Commander:");
        nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        TextField nameInput = new TextField(gameEngine.getPlayerName());
        nameInput.setPromptText("Enter Commander Name");
        nameInput.setPrefWidth(140);

        Button setNameBtn = new Button("Set Player");
        setNameBtn.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-cursor: hand;");
        setNameBtn.setOnAction(e -> {
            String input = nameInput.getText();
            if (input == null || input.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Commander Name", "Commander name cannot be blank!");
            } else {
                gameEngine.setPlayerName(input.trim());
                gameEngine.addNotification("Active commander set to: " + input.trim());
                refreshHUD();
                showAlert(Alert.AlertType.INFORMATION, "Commander Updated", "Active commander set to: " + input.trim());
            }
        });

        Label diffLabel = new Label("Difficulty:");
        diffLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        ComboBox<String> diffCombo = new ComboBox<>();
        diffCombo.getItems().addAll("Easy", "Normal", "Hard");
        diffCombo.setValue(gameEngine.getDifficulty());
        diffCombo.setOnAction(e -> {
            gameEngine.setDifficulty(diffCombo.getValue());
            gameEngine.addNotification("Difficulty adjusted to: " + diffCombo.getValue());
            refreshHUD();
        });

        setupRow.getChildren().addAll(nameLabel, nameInput, setNameBtn, diffLabel, diffCombo);

        // Row 2: Live Status HUD
        HBox hudRow = new HBox(20);
        hudRow.setAlignment(Pos.CENTER_LEFT);

        goldLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 14px; -fx-font-weight: bold;");
        livesLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-size: 14px; -fx-font-weight: bold;");
        waveLabel.setStyle("-fx-text-fill: #69f0ae; -fx-font-size: 14px; -fx-font-weight: bold;");
        scoreLabel.setStyle("-fx-text-fill: #40c4ff; -fx-font-size: 14px; -fx-font-weight: bold;");
        threadStatusLabel.setStyle("-fx-text-fill: #b0bec5; -fx-font-size: 13px; -fx-font-style: italic;");

        hudRow.getChildren().addAll(goldLabel, livesLabel, waveLabel, scoreLabel, threadStatusLabel);

        container.getChildren().addAll(setupRow, hudRow);
        return container;
    }

    /**
     * Builds Tab 1: The interactive 5x5 Battlefield view.
     */
    private BorderPane createBattlefieldView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(16));

        // Left Panel: Controls
        VBox controlPanel = new VBox(14);
        controlPanel.setPrefWidth(220);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #2b2b40; -fx-background-radius: 8; -fx-border-color: #3e3e5c; -fx-border-radius: 8;");

        Label controlTitle = new Label("Tactical Command");
        controlTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label buildLabel = new Label("Select Tower Type:");
        buildLabel.setStyle("-fx-text-fill: #cfd8dc; -fx-font-size: 12px;");

        towerTypeCombo.getItems().addAll(
                "Archer Tower (50g)",
                "Cannon Tower (90g)",
                "Mage Tower (75g)"
        );
        towerTypeCombo.setValue("Archer Tower (50g)");
        towerTypeCombo.setMaxWidth(Double.MAX_VALUE);

        upgradeModeToggle.setMaxWidth(Double.MAX_VALUE);
        upgradeModeToggle.setStyle("-fx-background-color: #455a64; -fx-text-fill: white; -fx-cursor: hand;");
        upgradeModeToggle.setOnAction(e -> {
            if (upgradeModeToggle.isSelected()) {
                upgradeModeToggle.setText("Mode: UPGRADE");
                upgradeModeToggle.setStyle("-fx-background-color: #ff9800; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                upgradeModeToggle.setText("Mode: BUILD");
                upgradeModeToggle.setStyle("-fx-background-color: #455a64; -fx-text-fill: white; -fx-cursor: hand;");
            }
        });

        Button startWaveBtn = new Button("▶ Start Next Wave");
        startWaveBtn.setMaxWidth(Double.MAX_VALUE);
        startWaveBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");
        startWaveBtn.setOnAction(e -> handleStartWave());

        Button resetBtn = new Button("↺ Reset Game");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> handleResetGame());

        Label instructions = new Label("Grid Rules:\n• Row 2 is Enemy Road.\n• Click grass to build.\n• Toggle Upgrade mode to level up towers.");
        instructions.setWrapText(true);
        instructions.setStyle("-fx-text-fill: #90a4ae; -fx-font-size: 11px;");

        controlPanel.getChildren().addAll(
                controlTitle,
                buildLabel,
                towerTypeCombo,
                upgradeModeToggle,
                startWaveBtn,
                resetBtn,
                instructions
        );

        // Center: 5x5 Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Button cellBtn = new Button();
                cellBtn.setPrefSize(110, 85);
                cellBtn.setMinSize(95, 75);
                final int row = r;
                final int col = c;
                cellBtn.setOnAction(e -> handleGridCellClick(row, col));
                gridButtons[r][c] = cellBtn;
                grid.add(cellBtn, c, r);
            }
        }

        // Right: Notifications ListView
        VBox notifPanel = new VBox(8);
        notifPanel.setPrefWidth(280);
        notifPanel.setPadding(new Insets(10));
        notifPanel.setStyle("-fx-background-color: #2b2b40; -fx-background-radius: 8; -fx-border-color: #3e3e5c; -fx-border-radius: 8;");

        Label notifTitle = new Label("Combat & System Log");
        notifTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

        notificationListView.setItems(notificationItems);
        notificationListView.setStyle("-fx-background-color: #1e1e2f; -fx-control-inner-background: #1e1e2f; -fx-text-fill: #cfd8dc; -fx-font-family: monospace; -fx-font-size: 11px;");
        VBox.setVgrow(notificationListView, Priority.ALWAYS);

        notifPanel.getChildren().addAll(notifTitle, notificationListView);

        pane.setLeft(controlPanel);
        pane.setCenter(grid);
        pane.setRight(notifPanel);

        return pane;
    }

    /**
     * Builds Tab 2: Armory directory with TableView and dynamic search.
     */
    private BorderPane createArmoryView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(16));

        // Search Bar
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 0, 12, 0));

        Label searchLabel = new Label("Search Towers:");
        searchLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        searchField.setPromptText("Enter name, ID (e.g. T1), or coords");
        searchField.setPrefWidth(220);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> filterArmoryTable(searchField.getText()));

        Button showAllBtn = new Button("Show All");
        showAllBtn.setStyle("-fx-background-color: #546e7a; -fx-text-fill: white; -fx-cursor: hand;");
        showAllBtn.setOnAction(e -> {
            searchField.clear();
            refreshArmoryTable();
        });

        searchBar.getChildren().addAll(searchLabel, searchField, searchBtn, showAllBtn);
        pane.setTop(searchBar);

        // TableView Configuration
        armoryTable.setItems(armoryData);
        armoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Tower, String> idCol = new TableColumn<>("Tower ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(70);

        TableColumn<Tower, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);

        TableColumn<Tower, Integer> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("level"));
        levelCol.setPrefWidth(60);

        TableColumn<Tower, Integer> dmgCol = new TableColumn<>("Damage");
        dmgCol.setCellValueFactory(new PropertyValueFactory<>("damage"));
        dmgCol.setPrefWidth(70);

        TableColumn<Tower, Integer> rangeCol = new TableColumn<>("Range");
        rangeCol.setCellValueFactory(new PropertyValueFactory<>("range"));
        rangeCol.setPrefWidth(65);

        TableColumn<Tower, Integer> costCol = new TableColumn<>("Upgrade Cost");
        costCol.setCellValueFactory(new PropertyValueFactory<>("upgradeCost"));
        costCol.setPrefWidth(100);

        TableColumn<Tower, String> coordsCol = new TableColumn<>("Coordinates");
        coordsCol.setCellValueFactory(new PropertyValueFactory<>("coordinates"));
        coordsCol.setPrefWidth(90);

        TableColumn<Tower, String> effectCol = new TableColumn<>("Special Effect");
        effectCol.setCellValueFactory(new PropertyValueFactory<>("specialEffectDescription"));
        effectCol.setPrefWidth(280);

        armoryTable.getColumns().addAll(idCol, nameCol, levelCol, dmgCol, rangeCol, costCol, coordsCol, effectCol);
        pane.setCenter(armoryTable);

        return pane;
    }

    /**
     * Builds Tab 3: Match history table and asynchronous background report generator.
     */
    private BorderPane createReportsView() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(16));

        VBox contentBox = new VBox(14);

        // Top Section: Match History TableView
        Label historyTitle = new Label("Wave & Match History");
        historyTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 15px;");

        historyTable.setItems(historyData);
        historyTable.setPrefHeight(220);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GameRecord, Integer> waveCol = new TableColumn<>("Wave #");
        waveCol.setCellValueFactory(new PropertyValueFactory<>("waveNumber"));
        waveCol.setPrefWidth(60);

        TableColumn<GameRecord, String> playerCol = new TableColumn<>("Commander");
        playerCol.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        playerCol.setPrefWidth(110);

        TableColumn<GameRecord, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        diffCol.setPrefWidth(80);

        TableColumn<GameRecord, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(70);

        TableColumn<GameRecord, Integer> livesCol = new TableColumn<>("Lives Left");
        livesCol.setCellValueFactory(new PropertyValueFactory<>("remainingLives"));
        livesCol.setPrefWidth(80);

        TableColumn<GameRecord, String> statusCol = new TableColumn<>("Result");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        statusCol.setPrefWidth(100);

        TableColumn<GameRecord, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(90);

        historyTable.getColumns().addAll(waveCol, playerCol, diffCol, scoreCol, livesCol, statusCol, timeCol);

        // Bottom Section: Async Report Generator
        Label reportTitle = new Label("Combat & System Analytics Report");
        reportTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 15px;");

        HBox reportBtnRow = new HBox(12);
        reportBtnRow.setAlignment(Pos.CENTER_LEFT);

        Button genReportBtn = new Button("📄 Generate System & Combat Report (Background Thread)");
        genReportBtn.setStyle("-fx-background-color: #00897b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        genReportBtn.setOnAction(e -> handleGenerateReport());

        reportStatusLabel.setStyle("-fx-text-fill: #80cbc4; -fx-font-style: italic;");
        reportBtnRow.getChildren().addAll(genReportBtn, reportStatusLabel);

        reportArea.setEditable(false);
        reportArea.setPrefRowCount(12);
        reportArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-control-inner-background: #181824; -fx-text-fill: #80deea;");
        reportArea.setPromptText("Click the button above to compile and view the analytics report asynchronously.");

        contentBox.getChildren().addAll(historyTitle, historyTable, reportTitle, reportBtnRow, reportArea);
        pane.setCenter(contentBox);

        return pane;
    }

    // Event Handlers

    private void handleGridCellClick(int row, int col) {
        try {
            if (upgradeModeToggle.isSelected()) {
                // Upgrade Mode
                gameEngine.upgradeTowerAt(row, col);
            } else {
                // Build Mode
                String selectedType = towerTypeCombo.getValue();
                if (selectedType == null) {
                    selectedType = "Archer";
                }
                gameEngine.placeTower(selectedType, row, col);
            }
        } catch (InsufficientGoldException | InvalidPlacementException ex) {
            showAlert(Alert.AlertType.WARNING, "Action Denied", ex.getMessage());
        } finally {
            refreshHUD();
            refreshGrid();
            refreshArmoryTable();
        }
    }

    private void handleStartWave() {
        if (gameEngine.isWaveInProgress()) {
            showAlert(Alert.AlertType.WARNING, "Wave In Progress", "A wave is already currently advancing!");
            return;
        }

        threadStatusLabel.setText("Thread Status: Wave Worker Active");
        threadStatusLabel.setStyle("-fx-text-fill: #69f0ae; -fx-font-weight: bold;");

        WaveSpawnerTask task = new WaveSpawnerTask(
                gameEngine,
                () -> {
                    // Periodic UI refresh inside runLater
                    refreshHUD();
                    refreshGrid();
                    refreshArmoryTable();
                    refreshHistoryTable();
                },
                () -> {
                    // On wave completion
                    threadStatusLabel.setText("Thread Status: Idle");
                    threadStatusLabel.setStyle("-fx-text-fill: #b0bec5; -fx-font-style: italic;");
                    refreshHUD();
                    refreshGrid();
                    refreshHistoryTable();
                }
        );

        Thread waveThread = new Thread(task);
        waveThread.setDaemon(true);
        waveThread.start();
    }

    private void handleGenerateReport() {
        threadStatusLabel.setText("Thread Status: Report Worker Active");
        ReportGeneratorTask reportTask = new ReportGeneratorTask(
                gameEngine,
                status -> {
                    reportStatusLabel.setText("Report Status: " + status);
                    threadStatusLabel.setText("Thread Status: Idle");
                },
                reportText -> reportArea.setText(reportText)
        );

        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }

    private void handleResetGame() {
        gameEngine.resetGame();
        refreshHUD();
        refreshGrid();
        refreshArmoryTable();
        refreshHistoryTable();
        reportArea.clear();
        reportStatusLabel.setText("Report Status: Idle");
        showAlert(Alert.AlertType.INFORMATION, "Game Reset", "Battlefield restored to initial deployment state.");
    }

    // Refresh Helpers

    private void refreshHUD() {
        goldLabel.setText("💰 Gold: " + gameEngine.getGold());
        livesLabel.setText("❤️ Lives: " + gameEngine.getLives());
        waveLabel.setText("🌊 Wave: " + gameEngine.getCurrentWave());
        scoreLabel.setText("⭐ Score: " + gameEngine.getScore());

        // Update notifications list
        List<String> notifs = gameEngine.getNotifications();
        notificationItems.setAll(notifs);
        if (!notifs.isEmpty()) {
            notificationListView.scrollTo(notifs.size() - 1);
        }
    }

    private void refreshGrid() {
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Button btn = gridButtons[r][c];
                if (r == 2) {
                    // Enemy Path
                    Enemy occupant = null;
                    for (Enemy e : gameEngine.getActiveEnemies()) {
                        if (e.getCol() == c && !e.isDead()) {
                            occupant = e;
                            break;
                        }
                    }

                    if (occupant != null) {
                        btn.setText(String.format("👾 %s\n%d/%d HP%s",
                                occupant.getType(), occupant.getHealth(), occupant.getMaxHealth(),
                                occupant.isSlowed() ? "\n[SLOWED]" : ""));
                        btn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 2;");
                    } else {
                        btn.setText("🛣️ Path\n(Enemy Road)");
                        btn.setStyle("-fx-background-color: #d7ccc8; -fx-text-fill: #4e342e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #a1887f; -fx-border-width: 2;");
                    }
                } else {
                    // Buildable Grass
                    Tower tower = gameEngine.getTowerAt(r, c);
                    if (tower != null) {
                        btn.setText(String.format("🏰 %s L%d\nDMG: %d | RNG: %d\n(%d, %d)",
                                tower.getName().replace(" Tower", ""),
                                tower.getLevel(), tower.getDamage(), tower.getRange(), r, c));
                        btn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #66bb6a; -fx-border-width: 2; -fx-cursor: hand;");
                    } else {
                        btn.setText(String.format("🌱 Open\n(%d, %d)", r, c));
                        btn.setStyle("-fx-background-color: #37474f; -fx-text-fill: #90a4ae; -fx-font-size: 11px; -fx-border-color: #455a64; -fx-border-width: 1; -fx-cursor: hand;");
                    }
                }
            }
        }
    }

    private void refreshArmoryTable() {
        armoryData.setAll(gameEngine.getTowers());
    }

    private void filterArmoryTable(String keyword) {
        armoryData.setAll(gameEngine.searchTowers(keyword));
    }

    private void refreshHistoryTable() {
        historyData.setAll(gameEngine.getGameRecords());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
