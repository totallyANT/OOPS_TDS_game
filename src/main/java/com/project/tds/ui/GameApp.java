package com.project.tds.ui;

import com.project.tds.engine.GameEngine;
import com.project.tds.engine.GameEventListener;
import com.project.tds.engine.GameState;
import com.project.tds.engine.GameStatus;
import com.project.tds.exception.GameException;
import com.project.tds.model.Targetable;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.tower.ArcherTower;
import com.project.tds.model.tower.CannonTower;
import com.project.tds.model.tower.MageTower;
import com.project.tds.model.tower.SplashCannonTower;
import com.project.tds.model.tower.SpreadMageTower;
import com.project.tds.model.tower.TargetingStrategy;
import com.project.tds.model.tower.Tower;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Map;

public class GameApp extends Application {
    private static final int CELL = 80;
    private static final int SIZE = GameState.GRID_SIZE;
    private static final long SHOT_FLASH_MS = 150;

    // Dark tactical theme (Arknights-style): charcoal tiles, cyan/yellow accents, red hostiles
    private static final Color BG = Color.web("#14161a");
    private static final Color GROUND = Color.web("#3c424b");      // buildable tiles
    private static final Color PATH = Color.web("#1f2328");        // enemy route
    private static final Color GRID_LINE = Color.web("#0b0c0e");
    private static final Color CYAN = Color.web("#23c4e8");
    private static final Color YELLOW = Color.web("#ffd400");
    private static final Color RANGE = Color.web("#ff8c1a");
    private static final Color HOSTILE = Color.web("#e0393e");
    private static final Color MAGE_VIOLET = Color.web("#b07cff");

    private final GameState state = new GameState();
    private final GameEngine engine = new GameEngine(state);

    private final Label goldLabel = new Label();
    private final Label livesLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label selectedLabel = new Label("Click an empty tile to build");
    private final Label messageLabel = new Label();
    private final ToggleGroup towerChoice = new ToggleGroup();
    private final ComboBox<TargetingStrategy> targeting = new ComboBox<>();
    private final Button pauseBtn = new Button("Pause");

    private Tower selected;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(SIZE * CELL, SIZE * CELL);
        canvas.setOnMouseClicked(e -> onTileClicked((int) (e.getY() / CELL), (int) (e.getX() / CELL)));

        BorderPane root = new BorderPane(canvas);
        root.setRight(buildSidePanel());

        listenToEngine();
        startDrawing(canvas.getGraphicsContext2D());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // colours and fonts
        stage.setTitle("Tower Defence");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /** The panel on the right: stats, tower choice and buttons. */
    private VBox buildSidePanel() {
        RadioButton archer = towerButton("Archer", "Archer (" + ArcherTower.BASE_COST + "g) - fast");
        RadioButton cannon = towerButton("Cannon", "Cannon (" + CannonTower.BASE_COST + "g) - splash");
        RadioButton mage = towerButton("Mage", "Mage (" + MageTower.BASE_COST + "g) - slows");
        archer.setSelected(true);

        targeting.getItems().addAll(TargetingStrategy.values());
        targeting.setDisable(true);
        targeting.setOnAction(e -> {
            if (selected != null && targeting.getValue() != null) {
                selected.setTargetingStrategy(targeting.getValue());
            }
        });

        Button upgradeBtn = new Button("Upgrade selected");
        upgradeBtn.setOnAction(e -> onUpgrade());

        Button waveBtn = new Button("Start next wave");
        waveBtn.getStyleClass().add("wave-button");
        waveBtn.setOnAction(e -> onStartWave());

        pauseBtn.setOnAction(e -> onPause());

        Button newGameBtn = new Button("New game");
        newGameBtn.getStyleClass().add("new-game-button");
        newGameBtn.setOnAction(e -> onNewGame());

        Label buildLabel = new Label("DEPLOY");
        buildLabel.getStyleClass().add("heading");
        goldLabel.getStyleClass().add("gold");
        messageLabel.setWrapText(true);
        selectedLabel.setWrapText(true);

        VBox side = new VBox(10, goldLabel, livesLabel, waveLabel, scoreLabel,
                buildLabel, archer, cannon, mage,
                selectedLabel, new Label("Targeting:"), targeting, upgradeBtn,
                waveBtn, pauseBtn, newGameBtn, messageLabel);
        side.getStyleClass().add("side-panel");
        side.setPadding(new Insets(10));
        side.setPrefWidth(230);
        return side;
    }

    /**
     * The engine runs on its own thread and tells us about waves, wins and losses through
     * GameEventListener. Platform.runLater hands the work back to the JavaFX thread.
     */
    private void listenToEngine() {
        engine.addListener(new GameEventListener() {
            @Override
            public void onGameStateUpdated(GameState s) {
                // we redraw every frame anyway (see startDrawing)
            }

            @Override
            public void onWaveStarted(int waveNumber) {
            }

            @Override
            public void onWaveCompleted(int waveNumber) {
            }

            @Override
            public void onGameOver() {
                Platform.runLater(() -> showMessage("Game over! You reached wave " + state.getWaveNumber()
                        + ". Click New game to retry.", true));
            }

            @Override
            public void onVictory() {
                Platform.runLater(() -> showMessage("Victory! All " + GameState.TOTAL_WAVES
                        + " waves are cleared.", false));
            }

            @Override
            public void onGameLog(String message) {
                Platform.runLater(() -> showMessage(message, false));
            }
        });
    }

    /** Redraws the board and updates the labels every frame (runs on the JavaFX thread). */
    private void startDrawing(GraphicsContext g) {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(g);
                goldLabel.setText("Gold: " + state.getGold());
                livesLabel.setText("Lives: " + state.getLives());
                waveLabel.setText("Wave: " + state.getWaveNumber() + " / " + GameState.TOTAL_WAVES);
                scoreLabel.setText("Score: " + state.getScore());
            }
        }.start();
    }

    @Override
    public void stop() {
        engine.stop();
    }

    private RadioButton towerButton(String type, String text) {
        RadioButton b = new RadioButton(text);
        b.setUserData(type);
        b.setToggleGroup(towerChoice);
        return b;
    }

    private void onTileClicked(int row, int col) {
        Tower existing = engine.getTowerAt(row, col);
        if (existing != null) {
            select(existing);
            return;
        }
        try {
            String type = (String) towerChoice.getSelectedToggle().getUserData();
            Tower tower = newTower(type, row, col);
            engine.placeTower(tower);
            select(tower);
            showMessage(type + " built", false);
        } catch (GameException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private Tower newTower(String type, int row, int col) throws GameException {
        switch (type) {
            case "Archer": return new ArcherTower(row, col);
            case "Cannon": return new SplashCannonTower(row, col);
            default:       return new SpreadMageTower(row, col);
        }
    }

    private void onStartWave() {
        try {
            engine.launchNextWave();
        } catch (GameException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private void onPause() {
        if (engine.isPaused()) {
            engine.resume();
            pauseBtn.setText("Pause");
        } else {
            engine.pause();
            if (engine.isPaused()) {
                pauseBtn.setText("Resume");
            }
        }
    }

    private void onNewGame() {
        engine.restart();
        select(null);
        pauseBtn.setText("Pause");
        showMessage("New game started", false);
    }

    private void onUpgrade() {
        if (selected == null) {
            showMessage("Click a tower first", true);
            return;
        }
        try {
            engine.upgradeTower(selected);
            select(selected);
            showMessage("Upgraded!", false);
        } catch (GameException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    /** Remembers the clicked tower and shows its stats (null = nothing selected). */
    private void select(Tower t) {
        selected = t;
        targeting.setDisable(t == null);
        if (t == null) {
            selectedLabel.setText("Click an empty tile to build");
            return;
        }
        targeting.setValue(t.getTargetingStrategy());
        String stats = String.format("Damage %d, %.1f shots/s, range %.1f", t.getDamage(), t.getFireRate(), t.getRange());
        if (t instanceof MageTower) {
            stats += String.format(", slow %.1fs", ((MageTower) t).getSlowDurationMs() / 1000.0);
        }
        String upgrade = t.isMaxLevel() ? "Max level" : "Upgrade: " + t.getUpgradeCost() + "g";
        selectedLabel.setText(t.getName() + " (level " + t.getLevel() + "/" + Tower.MAX_LEVEL + ")\n"
                + stats + "\n" + upgrade);
    }

    private void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        // inline style, because style.css already sets a text colour for every label
        messageLabel.setStyle("-fx-text-fill: " + (error ? "#e0393e" : "#23c4e8") + ";");
    }

    private void draw(GraphicsContext g) {
        g.setFill(BG);
        g.fillRect(0, 0, SIZE * CELL, SIZE * CELL);

        // tiles: raised grey squares for buildable ground, dark sunken lane for the path
        g.setLineWidth(2);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                boolean path = engine.getPath().isPathCell(r, c);
                g.setFill(path ? PATH : GROUND);
                g.fillRect(c * CELL + 2, r * CELL + 2, CELL - 4, CELL - 4);
                g.setStroke(path ? GRID_LINE : Color.web("#5a626d"));
                g.strokeRect(c * CELL + 2, r * CELL + 2, CELL - 4, CELL - 4);
            }
        }

        // range circle for the selected tower (orange like an attack range)
        if (selected != null && state.getActiveTowers().contains(selected)) {
            double radius = selected.getRange() * CELL;
            double cx = selected.getCol() * CELL + CELL / 2.0;
            double cy = selected.getRow() * CELL + CELL / 2.0;
            g.setFill(Color.web("#ff8c1a", 0.15));   // same orange, 15% see-through
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setStroke(RANGE);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // towers drawn after the range so they stay on top
        for (Object o : state.getActiveTowers()) {
            Tower t = (Tower) o;
            drawTower(g, t, t == selected);
        }

        // enemies
        for (Targetable t : state.getActiveEnemies()) {
            drawEnemy(g, (Enemy) t);
        }

        // shot flashes on top of everything
        long now = System.currentTimeMillis();
        for (Map.Entry<Tower, GameEngine.Shot> entry : engine.getLastShots().entrySet()) {
            if (now - entry.getValue().timeMs() < SHOT_FLASH_MS && state.getActiveTowers().contains(entry.getKey())) {
                drawShot(g, entry.getKey(), entry.getValue());
            }
        }
    }

    private Color towerColor(Tower t) {
        if (t instanceof ArcherTower) return CYAN;
        if (t instanceof CannonTower) return Color.web("#ff8c1a");
        return MAGE_VIOLET;
    }

    /** A short flash from the tower to whatever it just hit, so you can see every shot. */
    private void drawShot(GraphicsContext g, Tower t, GameEngine.Shot shot) {
        double fromX = t.getCol() * CELL + CELL / 2.0;
        double fromY = t.getRow() * CELL + CELL / 2.0;
        double toX = shot.x() * CELL + CELL / 2.0;
        double toY = shot.y() * CELL + CELL / 2.0;
        g.setStroke(towerColor(t));
        if (t instanceof ArcherTower) {
            g.setLineWidth(2);   // thin arrow line
            g.strokeLine(fromX, fromY, toX, toY);
        } else if (t instanceof CannonTower) {
            double r = SplashCannonTower.SPLASH_RADIUS * CELL;   // blast circle shows the splash area
            g.setFill(Color.web("#ff8c1a", 0.35));   // see-through orange blast
            g.fillOval(toX - r, toY - r, r * 2, r * 2);
            g.setLineWidth(2);
            g.strokeOval(toX - r, toY - r, r * 2, r * 2);
        } else {
            g.setLineWidth(5);   // thick magic beam
            g.strokeLine(fromX, fromY, toX, toY);
        }
    }

    private void drawTower(GraphicsContext g, Tower t, boolean isSelected) {
        Color color = towerColor(t);
        double x = t.getCol() * CELL + 15;
        double y = t.getRow() * CELL + 15;
        double size = CELL - 30;
        // dark body with a coloured frame, like an operator icon
        g.setFill(Color.web("#22262c"));
        g.fillRect(x, y, size, size);
        g.setLineWidth(3);
        g.setStroke(isSelected ? YELLOW : color);
        g.strokeRect(x, y, size, size);
        g.setFill(color);
        g.fillRect(x, y + size - 8, size, 8);   // class colour strip along the bottom
        g.setFill(Color.WHITE);
        g.fillText(t.getName().substring(0, 1), x + 21, y + 28);
        // one yellow pip per upgrade level
        g.setFill(YELLOW);
        for (int i = 0; i < t.getLevel(); i++) {
            g.fillRect(x + 4 + i * 12, y - 10, 7, 7);
        }
    }

    private void drawEnemy(GraphicsContext g, Enemy e) {
        Color color;
        double size;
        switch (e.getName()) {
            case "Goblin":      color = Color.web("#c0484b"); size = 20; break;
            case "Orc":         color = Color.web("#8a1f24"); size = 28; break;
            case "Slime":       color = Color.web("#d9603b"); size = 24; break;
            case "Small Slime": color = Color.web("#e8845f"); size = 14; break;
            case "Frost Golem": color = Color.web("#8fd3e8"); size = 28; break;
            default:            color = Color.web("#2a0d10"); size = 40; break;
        }
        double cx = e.getX() * CELL + CELL / 2.0;
        double cy = e.getY() * CELL + CELL / 2.0;
        g.setFill(e.isSlowed() ? color.interpolate(MAGE_VIOLET, 0.5) : color);
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
        g.setLineWidth(2);
        g.setStroke(HOSTILE);
        g.strokeOval(cx - size / 2, cy - size / 2, size, size);
        if (e.isSlowed()) {
            // violet ring around slowed enemies so the Mage's effect is easy to see
            g.setLineWidth(3);
            g.setStroke(MAGE_VIOLET);
            g.strokeOval(cx - size / 2 - 5, cy - size / 2 - 5, size + 10, size + 10);
        }

        // health bar
        double hpWidth = 30.0 * e.getHealthPercentage();
        g.setFill(Color.web("#0b0c0e"));
        g.fillRect(cx - 15, cy - size / 2 - 8, 30, 4);
        g.setFill(HOSTILE);
        g.fillRect(cx - 15, cy - size / 2 - 8, hpWidth, 4);
    }
}
