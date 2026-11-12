package td;

import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
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

/**
 * Programmatic JavaFX view. All layout and styling lives in Java; no FXML/CSS files.
 */
public class TowerDefenseApp extends Application {

    private static final int BOARD_SIZE = 5;
    private static final int PATH_ROW = 2;

    private static final String GRASS_STYLE = """
            -fx-background-color: #14532d;
            -fx-border-color: #22c55e;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-text-fill: #f0fdf4;
            """;
    private static final String PATH_STYLE = """
            -fx-background-color: #78350f;
            -fx-border-color: #f59e0b;
            -fx-border-width: 2;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-text-fill: #fef3c7;
            """;
    private static final String TOWER_STYLE = """
            -fx-background-color: #1e3a8a;
            -fx-border-color: #60a5fa;
            -fx-border-width: 2;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-text-fill: #eff6ff;
            """;

    private final GameEngine engine = new GameEngine();
    private final Button[][] cellButtons = new Button[BOARD_SIZE][BOARD_SIZE];

    private final Label goldLabel = new Label();
    private final Label livesLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label commanderLabel = new Label();
    private final Label threadStatusLabel = new Label();
    private final Label reportStatusLabel = new Label("No report generated yet.");
    private final ListView<String> notificationList = new ListView<>();
    private final TableView<Tower> armoryTable = new TableView<>();
    private final TableView<GameRecord> historyTable = new TableView<>();
    private final TextArea reportArea = new TextArea();

    private Stage stage;
    private TextField nameField;
    private TextField searchField;
    private ComboBox<String> difficultyBox;
    private ComboBox<String> towerBox;
    private ToggleButton upgradeToggle;
    private String armoryFilter = "";
    private boolean reportRunning = false;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0b1220;");
        root.setTop(buildTopBar());
        root.setCenter(buildTabs());

        Scene scene = new Scene(root, 1320, 880);
        primaryStage.setTitle("Tower Defense - OOP Edition");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1150);
        primaryStage.setMinHeight(760);
        primaryStage.show();

        refreshAll();
    }

    private VBox buildTopBar() {
        VBox top = new VBox(8);
        top.setPadding(new Insets(12, 16, 12, 16));
        top.setStyle("-fx-background-color: #111c31; -fx-border-color: #1e3a8a;"
                + " -fx-border-width: 0 0 2 0;");

        Label title = new Label("TOWER DEFENSE");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 20px; -fx-font-weight: bold;");

        nameField = new TextField(engine.getPlayerName());
        nameField.setPromptText("Commander name");
        nameField.setPrefWidth(170);

        Button setNameButton = new Button("Set Player");
        setNameButton.setOnAction(event -> handleSetPlayer());

        difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll("Easy", "Normal", "Hard");
        difficultyBox.setValue(engine.getDifficulty());
        difficultyBox.setOnAction(event -> {
            String selected = difficultyBox.getValue();
            if (selected != null) {
                engine.setDifficulty(selected);
                engine.addNotification("Difficulty set to " + selected + ".");
                refreshAll();
            }
        });

        HBox setupBar = new HBox(10,
                title,
                new Separator(),
                styledLabel("Commander:", "#cbd5e1"),
                nameField,
                setNameButton,
                styledLabel("Difficulty:", "#cbd5e1"),
                difficultyBox);
        setupBar.setAlignment(Pos.CENTER_LEFT);

        commanderLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold;");
        goldLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        livesLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        waveLabel.setStyle("-fx-text-fill: #c4b5fd; -fx-font-weight: bold;");
        scoreLabel.setStyle("-fx-text-fill: #67e8f9; -fx-font-weight: bold;");
        threadStatusLabel.setStyle("-fx-text-fill: #86efac; -fx-font-weight: bold;");

        HBox hudBar = new HBox(22,
                commanderLabel, goldLabel, livesLabel, waveLabel, scoreLabel, threadStatusLabel);
        hudBar.setAlignment(Pos.CENTER_LEFT);

        top.getChildren().addAll(setupBar, hudBar);
        return top;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab battlefieldTab = new Tab("Battlefield", buildBattlefieldTab());
        Tab armoryTab = new Tab("Armory & Bestiary", buildArmoryTab());
        Tab historyTab = new Tab("Match History & Reports", buildHistoryTab());
        tabs.getTabs().addAll(battlefieldTab, armoryTab, historyTab);
        return tabs;
    }

    private BorderPane buildBattlefieldTab() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(14));
        pane.setLeft(buildControlPanel());
        pane.setCenter(buildGridPanel());
        pane.setRight(buildNotificationPanel());
        return pane;
    }

    private VBox buildControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #111c31; -fx-background-radius: 8;"
                + " -fx-border-color: #1e293b; -fx-border-radius: 8;");

        Label heading = sectionHeading("BUILD MENU");

        Label towerLabel = styledLabel("Tower type", "#cbd5e1");
        towerBox = new ComboBox<>();
        towerBox.getItems().addAll(
                "Archer Tower (50g)",
                "Mage Tower (75g)",
                "Cannon Tower (90g)");
        towerBox.setValue(towerBox.getItems().get(0));
        towerBox.setMaxWidth(Double.MAX_VALUE);

        Button startWaveButton = new Button("Start Next Wave");
        startWaveButton.setMaxWidth(Double.MAX_VALUE);
        startWaveButton.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white;"
                + " -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10;");
        startWaveButton.setOnAction(event -> handleStartWave());

        upgradeToggle = new ToggleButton("Upgrade Mode: OFF");
        upgradeToggle.setMaxWidth(Double.MAX_VALUE);
        upgradeToggle.setStyle("-fx-padding: 8;");
        upgradeToggle.setOnAction(event -> {
            if (upgradeToggle.isSelected()) {
                upgradeToggle.setText("Upgrade Mode: ON");
                upgradeToggle.setStyle("-fx-background-color: #15803d; -fx-text-fill: white;"
                        + " -fx-font-weight: bold; -fx-padding: 8;");
            } else {
                upgradeToggle.setText("Upgrade Mode: OFF");
                upgradeToggle.setStyle("-fx-padding: 8;");
            }
        });

        Button resetButton = new Button("Reset Game");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(event -> handleResetGame());

        Label legend = new Label("""
                PATH: row 2 (amber)
                BUILD: rows 0, 1, 3, 4

                Click a grass cell to build.
                Toggle upgrade mode, then click
                a tower to upgrade it.

                Towers attack every tick while
                an enemy is within Manhattan range.
                """);
        legend.setWrapText(true);
        legend.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        panel.getChildren().addAll(
                heading, towerLabel, towerBox, new Separator(),
                startWaveButton, upgradeToggle, resetButton, new Separator(), legend);
        return panel;
    }

    private VBox buildGridPanel() {
        VBox wrapper = new VBox(10);
        wrapper.setAlignment(Pos.CENTER);

        Label heading = sectionHeading("5 x 5 BATTLEFIELD");
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button cell = new Button();
                cell.setMinSize(112, 86);
                cell.setPrefSize(112, 86);
                cell.setWrapText(true);
                cell.setStyle(GRASS_STYLE);
                final int clickedRow = row;
                final int clickedCol = col;
                cell.setOnAction(event -> handleCellClick(clickedRow, clickedCol));
                cellButtons[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        Label pathNote = new Label("Row 2 is the enemy path. All other rows are buildable grass.");
        pathNote.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        StackPane center = new StackPane(grid);
        center.setPadding(new Insets(0, 14, 0, 14));
        wrapper.getChildren().addAll(heading, center, pathNote);
        return wrapper;
    }

    private VBox buildNotificationPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(14, 0, 0, 14));
        panel.setPrefWidth(310);

        Label heading = sectionHeading("NOTIFICATION CENTER");
        notificationList.setStyle("-fx-background-color: #0f172a; -fx-control-inner-background: #0f172a;"
                + " -fx-border-color: #1e293b;");
        notificationList.setPlaceholder(styledLabel("Combat events will appear here.", "#64748b"));
        notificationList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setWrapText(true);
                    setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
                }
            }
        });
        VBox.setVgrow(notificationList, Priority.ALWAYS);
        panel.getChildren().addAll(heading, notificationList);
        return panel;
    }

    private VBox buildArmoryTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14));

        Label heading = sectionHeading("ARMORY & TOWER DIRECTORY");

        searchField = new TextField();
        searchField.setPromptText("Search by name, tower ID, or row,col (for example 1,3)");
        searchField.setPrefWidth(420);
        searchField.setOnAction(event -> handleSearch());

        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> handleSearch());

        Button showAllButton = new Button("Show All");
        showAllButton.setOnAction(event -> {
            armoryFilter = "";
            searchField.clear();
            refreshArmory();
        });

        HBox searchBar = new HBox(8, searchField, searchButton, showAllButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        armoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        armoryTable.setPlaceholder(styledLabel("No towers match the current search.", "#64748b"));

        TableColumn<Tower, String> idColumn = new TableColumn<>("Tower ID");
        idColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getId()));

        TableColumn<Tower, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getName()));

        TableColumn<Tower, String> levelColumn = new TableColumn<>("Level");
        levelColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getLevel())));

        TableColumn<Tower, String> damageColumn = new TableColumn<>("Damage");
        damageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getDamage())));

        TableColumn<Tower, String> rangeColumn = new TableColumn<>("Range");
        rangeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getRange())));

        TableColumn<Tower, String> upgradeColumn = new TableColumn<>("Upgrade Cost");
        upgradeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getUpgradeCost() + "g"));

        TableColumn<Tower, String> coordinatesColumn = new TableColumn<>("Coordinates");
        coordinatesColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                "(" + data.getValue().getRow() + ", " + data.getValue().getCol() + ")"));

        TableColumn<Tower, String> effectColumn = new TableColumn<>("Special Effect");
        effectColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSpecialEffectDescription()));

        armoryTable.getColumns().addAll(List.of(idColumn, nameColumn, levelColumn, damageColumn,
                rangeColumn, upgradeColumn, coordinatesColumn, effectColumn));

        VBox.setVgrow(armoryTable, Priority.ALWAYS);
        box.getChildren().addAll(heading, searchBar, armoryTable);
        return box;
    }

    private VBox buildHistoryTab() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14));

        Label heading = sectionHeading("MATCH HISTORY & BACKGROUND REPORTS");

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historyTable.setPlaceholder(styledLabel("No completed waves yet.", "#64748b"));
        historyTable.setPrefHeight(240);

        TableColumn<GameRecord, String> waveColumn = new TableColumn<>("Wave #");
        waveColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getWaveNumber())));

        TableColumn<GameRecord, String> playerColumn = new TableColumn<>("Commander");
        playerColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getPlayerName()));

        TableColumn<GameRecord, String> difficultyColumn = new TableColumn<>("Difficulty");
        difficultyColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getDifficulty()));

        TableColumn<GameRecord, String> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getScore())));

        TableColumn<GameRecord, String> livesColumn = new TableColumn<>("Remaining Lives");
        livesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getRemainingLives())));

        TableColumn<GameRecord, String> resultColumn = new TableColumn<>("Status");
        resultColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getResult()));

        TableColumn<GameRecord, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getTimestamp()));

        historyTable.getColumns().addAll(List.of(waveColumn, playerColumn, difficultyColumn,
                scoreColumn, livesColumn, resultColumn, timestampColumn));

        Button generateButton = new Button("Generate System & Combat Report (Background Thread)");
        generateButton.setStyle("-fx-background-color: #6d28d9; -fx-text-fill: white;"
                + " -fx-font-weight: bold; -fx-padding: 9;");
        generateButton.setOnAction(event -> handleGenerateReport());

        reportStatusLabel.setStyle("-fx-text-fill: #c4b5fd; -fx-font-weight: bold;");

        HBox reportBar = new HBox(12, generateButton, reportStatusLabel);
        reportBar.setAlignment(Pos.CENTER_LEFT);

        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setPrefRowCount(14);
        reportArea.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 12px;"
                + " -fx-control-inner-background: #0f172a; -fx-text-fill: #e2e8f0;");
        reportArea.setPromptText("Generate a report to analyze towers and wave history.");

        ScrollPane reportScroll = new ScrollPane(reportArea);
        reportScroll.setFitToWidth(true);
        reportScroll.setFitToHeight(true);
        VBox.setVgrow(reportScroll, Priority.ALWAYS);

        box.getChildren().addAll(heading, historyTable, reportBar, reportScroll);
        return box;
    }

    private void handleSetPlayer() {
        try {
            engine.setPlayerName(nameField.getText());
            refreshAll();
        } catch (InvalidPlacementException exception) {
            showWarning("Invalid Commander", exception.getMessage());
        }
    }

    private void handleStartWave() {
        try {
            engine.beginWave();
            refreshAll();
            Thread waveThread = new Thread(
                    new WaveSpawnerTask(engine, this::refreshAll), "wave-spawner-thread");
            waveThread.setDaemon(true);
            waveThread.start();
        } catch (InvalidPlacementException exception) {
            showWarning("Cannot Start Wave", exception.getMessage());
        }
    }

    private void handleResetGame() {
        if (engine.isWaveInProgress()) {
            showWarning("Wave Running", "Wait for the current wave to finish before resetting.");
            return;
        }
        engine.resetGame();
        reportArea.clear();
        reportStatusLabel.setText("No report generated yet.");
        armoryFilter = "";
        if (searchField != null) {
            searchField.clear();
        }
        refreshAll();
    }

    private void handleCellClick(int row, int col) {
        try {
            if (upgradeToggle.isSelected()) {
                engine.upgradeTowerAt(row, col);
            } else {
                engine.placeTower(selectedTowerType(), row, col);
            }
        } catch (InsufficientGoldException | InvalidPlacementException exception) {
            showWarning("Action Blocked", exception.getMessage());
        }
        refreshAll();
    }

    private void handleSearch() {
        armoryFilter = searchField.getText() == null ? "" : searchField.getText();
        refreshArmory();
    }

    private void handleGenerateReport() {
        if (reportRunning) {
            reportStatusLabel.setText("A report is already being generated...");
            return;
        }
        reportRunning = true;
        reportStatusLabel.setText("Generating report in background...");
        Thread reportThread = new Thread(new ReportGeneratorTask(
                engine,
                reportArea::setText,
                status -> {
                    reportStatusLabel.setText(status);
                    if (!status.startsWith("Generating")) {
                        reportRunning = false;
                    }
                }), "report-generator-thread");
        reportThread.setDaemon(true);
        reportThread.start();
    }

    private String selectedTowerType() {
        String selected = towerBox.getValue();
        if (selected == null) {
            return null;
        }
        if (selected.startsWith("Archer")) {
            return "Archer Tower";
        }
        if (selected.startsWith("Mage")) {
            return "Mage Tower";
        }
        return "Cannon Tower";
    }

    private void refreshAll() {
        goldLabel.setText("Gold: " + engine.getGold() + "g");
        livesLabel.setText("Lives: " + engine.getLives());
        waveLabel.setText("Wave: " + engine.getCurrentWave());
        scoreLabel.setText("Score: " + engine.getScore());
        commanderLabel.setText("Commander: " + engine.getPlayerName());
        threadStatusLabel.setText("Wave thread: "
                + (engine.isWaveInProgress() ? "RUNNING" : "IDLE"));
        refreshGrid();
        refreshNotifications();
        refreshArmory();
        refreshHistory();
    }

    private void refreshGrid() {
        List<Enemy> enemies = engine.getActiveEnemies();
        List<Tower> towers = engine.getTowers();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button cell = cellButtons[row][col];
                if (row == PATH_ROW) {
                    StringBuilder text = new StringBuilder();
                    for (Enemy enemy : enemies) {
                        if (enemy.getCol() == col) {
                            text.append(enemy.getType())
                                    .append(" [").append(enemy.getHealth()).append(" HP]\n");
                        }
                    }
                    cell.setText(text.length() == 0 ? "PATH" : text.toString().trim());
                    cell.setStyle(PATH_STYLE);
                } else {
                    Tower tower = findTower(towers, row, col);
                    if (tower != null) {
                        cell.setText(tower.getName() + " L" + tower.getLevel()
                                + "\nDMG: " + tower.getDamage()
                                + "\nRNG: " + tower.getRange());
                        cell.setStyle(TOWER_STYLE);
                    } else {
                        cell.setText("Grass\n(" + row + "," + col + ")");
                        cell.setStyle(GRASS_STYLE);
                    }
                }
            }
        }
    }

    private Tower findTower(List<Tower> towers, int row, int col) {
        for (Tower tower : towers) {
            if (tower.getRow() == row && tower.getCol() == col) {
                return tower;
            }
        }
        return null;
    }

    private void refreshNotifications() {
        List<String> messages = engine.getNotifications();
        notificationList.getItems().setAll(messages);
        if (!messages.isEmpty()) {
            notificationList.scrollTo(messages.size() - 1);
        }
    }

    private void refreshArmory() {
        armoryTable.getItems().setAll(engine.searchTowers(armoryFilter));
    }

    private void refreshHistory() {
        historyTable.getItems().setAll(engine.getGameRecords());
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Tower Defense");
        alert.setHeaderText(title);
        alert.setContentText(message);
        if (stage != null) {
            alert.initOwner(stage);
        }
        alert.showAndWait();
    }

    private Label styledLabel(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }

    private Label sectionHeading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
