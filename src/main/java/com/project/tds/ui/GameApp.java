package com.project.tds.ui;

import com.project.tds.engine.GameLoop;
import com.project.tds.engine.GameState;
import com.project.tds.model.GameException;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.tower.Tower;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameApp extends Application {
    private static final int CELL = 80;

    // Dark tactical theme (Arknights-style): charcoal tiles, cyan/yellow accents, red hostiles
    private static final Color BG = Color.web("#14161a");
    private static final Color GROUND = Color.web("#3c424b");      // buildable tiles
    private static final Color PATH = Color.web("#1f2328");        // enemy route
    private static final Color GRID_LINE = Color.web("#0b0c0e");
    private static final Color CYAN = Color.web("#23c4e8");
    private static final Color YELLOW = Color.web("#ffd400");
    private static final Color RANGE = Color.web("#ff8c1a");
    private static final Color HOSTILE = Color.web("#e0393e");
    private static final String TEXT_STYLE = "-fx-text-fill: #e8e8e8; -fx-font-size: 13px;";
    private static final String BUTTON_STYLE = "-fx-background-color: #23c4e8; -fx-text-fill: #14161a;"
            + " -fx-font-weight: bold; -fx-background-radius: 0;";

    private final GameState state = new GameState();
    private final GameLoop loop = new GameLoop(state);

    private final Label goldLabel = new Label();
    private final Label livesLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label selectedLabel = new Label("Click an empty tile to build");
    private final Label messageLabel = new Label();
    private final ToggleGroup towerChoice = new ToggleGroup();

    private int selectedRow = -1;
    private int selectedCol = -1;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(GameState.SIZE * CELL, GameState.SIZE * CELL);
        canvas.setOnMouseClicked(e -> onTileClicked((int) (e.getY() / CELL), (int) (e.getX() / CELL)));

        RadioButton archer = towerButton("Archer", "Archer (50g) - fast");
        RadioButton cannon = towerButton("Cannon", "Cannon (80g) - heavy");
        RadioButton mage = towerButton("Mage", "Mage (70g) - slows");
        archer.setSelected(true);

        Button upgradeBtn = new Button("Upgrade selected");
        upgradeBtn.setOnAction(e -> onUpgrade());

        Button waveBtn = new Button("Start next wave");
        waveBtn.setOnAction(e -> {
            try {
                state.startWave();
                showMessage("Wave " + state.getWave() + " incoming!", false);
            } catch (GameException ex) {
                showMessage(ex.getMessage(), true);
            }
        });

        messageLabel.setWrapText(true);
        selectedLabel.setWrapText(true);
        Label buildLabel = new Label("DEPLOY");
        VBox side = new VBox(10, goldLabel, livesLabel, waveLabel,
                buildLabel, archer, cannon, mage,
                selectedLabel, upgradeBtn, waveBtn, messageLabel);
        side.setPadding(new Insets(10));
        side.setPrefWidth(220);
        side.setStyle("-fx-background-color: #1b1e23; -fx-border-color: #23c4e8; -fx-border-width: 0 0 0 3;");

        for (Labeled l : new Labeled[]{goldLabel, livesLabel, waveLabel, selectedLabel, archer, cannon, mage}) {
            l.setStyle(TEXT_STYLE);
        }
        goldLabel.setStyle(TEXT_STYLE + " -fx-text-fill: #ffd400; -fx-font-weight: bold;");
        buildLabel.setStyle("-fx-text-fill: #23c4e8; -fx-font-weight: bold; -fx-font-size: 14px;");
        upgradeBtn.setStyle(BUTTON_STYLE);
        waveBtn.setStyle(BUTTON_STYLE.replace("#23c4e8", "#ffd400"));

        BorderPane root = new BorderPane(canvas);
        root.setRight(side);
        root.setStyle("-fx-background-color: #14161a;");

        GraphicsContext g = canvas.getGraphicsContext2D();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(g);
                goldLabel.setText("Gold: " + state.getGold());
                livesLabel.setText("Lives: " + state.getLives());
                waveLabel.setText("Wave: " + state.getWave() + " / " + GameState.MAX_WAVES);
                if (state.isGameOver()) {
                    showMessage("Game over! You survived " + (state.getWave() - 1) + " waves.", true);
                } else if (state.isVictory()) {
                    showMessage("Victory! The Boss is down and all " + GameState.MAX_WAVES + " waves are cleared.", false);
                }
            }
        }.start();

        Thread gameThread = new Thread(loop, "game-loop");
        gameThread.setDaemon(true);
        gameThread.start();

        stage.setTitle("Tower Defence");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        loop.stop();
    }

    private RadioButton towerButton(String type, String text) {
        RadioButton b = new RadioButton(text);
        b.setUserData(type);
        b.setToggleGroup(towerChoice);
        return b;
    }

    private void onTileClicked(int row, int col) {
        if (state.getTower(row, col) != null) {
            selectedRow = row;
            selectedCol = col;
            updateSelectedLabel();
            return;
        }
        try {
            String type = (String) towerChoice.getSelectedToggle().getUserData();
            state.placeTower(type, row, col);
            selectedRow = row;
            selectedCol = col;
            updateSelectedLabel();
            showMessage(type + " built", false);
        } catch (GameException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private void onUpgrade() {
        if (selectedRow < 0) {
            showMessage("Click a tower first", true);
            return;
        }
        try {
            state.upgradeTower(selectedRow, selectedCol);
            updateSelectedLabel();
            showMessage("Upgraded!", false);
        } catch (GameException ex) {
            showMessage(ex.getMessage(), true);
        }
    }

    private void updateSelectedLabel() {
        Tower t = state.getTower(selectedRow, selectedCol);
        String upgrade = t.getLevel() < Tower.MAX_LEVEL ? "Upgrade: " + t.getUpgradeCost() + "g" : "Max level";
        selectedLabel.setText(t.getName() + " (level " + t.getLevel() + "/" + Tower.MAX_LEVEL + ")\n"
                + t.getStats() + "\n" + upgrade);
    }

    private void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.setTextFill(error ? HOSTILE : CYAN);
    }

    private void draw(GraphicsContext g) {
        g.setFill(BG);
        g.fillRect(0, 0, GameState.SIZE * CELL, GameState.SIZE * CELL);

        // tiles: raised grey squares for buildable ground, dark sunken lane for the path
        g.setLineWidth(2);
        for (int r = 0; r < GameState.SIZE; r++) {
            for (int c = 0; c < GameState.SIZE; c++) {
                boolean path = state.isPath(r, c);
                g.setFill(path ? PATH : GROUND);
                g.fillRect(c * CELL + 2, r * CELL + 2, CELL - 4, CELL - 4);
                g.setStroke(path ? GRID_LINE : Color.web("#5a626d"));
                g.strokeRect(c * CELL + 2, r * CELL + 2, CELL - 4, CELL - 4);
            }
        }

        // range circle for the selected tower (orange like an attack range)
        if (selectedRow >= 0) {
            Tower t = state.getTower(selectedRow, selectedCol);
            double radius = t.getRange() * CELL;
            double cx = selectedCol * CELL + CELL / 2.0;
            double cy = selectedRow * CELL + CELL / 2.0;
            g.setFill(RANGE.deriveColor(0, 1, 1, 0.15));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setStroke(RANGE);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // towers drawn after the range so they stay on top
        for (int r = 0; r < GameState.SIZE; r++) {
            for (int c = 0; c < GameState.SIZE; c++) {
                Tower t = state.getTower(r, c);
                if (t != null) {
                    drawTower(g, t, r == selectedRow && c == selectedCol);
                }
            }
        }

        // enemies
        for (Enemy e : state.getEnemies()) {
            drawEnemy(g, e);
        }
    }

    private void drawTower(GraphicsContext g, Tower t, boolean selected) {
        Color color;
        switch (t.getName()) {
            case "Archer": color = CYAN; break;
            case "Cannon": color = Color.web("#ff8c1a"); break;
            default:       color = Color.web("#b07cff"); break;
        }
        double x = t.getCol() * CELL + 15;
        double y = t.getRow() * CELL + 15;
        double size = CELL - 30;
        // dark body with a coloured frame, like an operator icon
        g.setFill(Color.web("#22262c"));
        g.fillRect(x, y, size, size);
        g.setLineWidth(3);
        g.setStroke(selected ? YELLOW : color);
        g.strokeRect(x, y, size, size);
        g.setFill(color);
        g.fillRect(x, y + size - 8, size, 8);   // class colour strip along the bottom
        g.setFill(Color.WHITE);
        g.fillText(t.getName().substring(0, 1), x + 21, y + 28);
        // one yellow diamond-ish pip per upgrade level
        g.setFill(YELLOW);
        for (int i = 0; i < t.getLevel(); i++) {
            g.fillRect(x + 4 + i * 12, y - 10, 7, 7);
        }
    }

    private void drawEnemy(GraphicsContext g, Enemy e) {
        Color color;
        double size;
        switch (e.getName()) {
            case "Goblin": color = Color.web("#c0484b"); size = 20; break;
            case "Orc":    color = Color.web("#8a1f24"); size = 28; break;
            case "Slime":  color = Color.web("#d9603b"); size = 24; break;
            default:       color = Color.web("#2a0d10"); size = 40; break;
        }
        double cx = e.getX() * CELL + CELL / 2.0;
        double cy = e.getY() * CELL + CELL / 2.0;
        g.setFill(e.isSlowed() ? color.interpolate(Color.web("#b07cff"), 0.5) : color);
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
        g.setLineWidth(2);
        g.setStroke(HOSTILE);
        g.strokeOval(cx - size / 2, cy - size / 2, size, size);

        // health bar
        double hpWidth = 30.0 * e.getHealth() / e.getMaxHealth();
        g.setFill(Color.web("#0b0c0e"));
        g.fillRect(cx - 15, cy - size / 2 - 8, 30, 4);
        g.setFill(HOSTILE);
        g.fillRect(cx - 15, cy - size / 2 - 8, hpWidth, 4);
    }
}
