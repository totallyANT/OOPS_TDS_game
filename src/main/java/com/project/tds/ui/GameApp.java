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
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameApp extends Application {
    private static final int CELL = 80;

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
        VBox side = new VBox(10, goldLabel, livesLabel, waveLabel,
                new Label("Build:"), archer, cannon, mage,
                selectedLabel, upgradeBtn, waveBtn, messageLabel);
        side.setPadding(new Insets(10));
        side.setPrefWidth(220);

        BorderPane root = new BorderPane(canvas);
        root.setRight(side);

        GraphicsContext g = canvas.getGraphicsContext2D();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(g);
                goldLabel.setText("Gold: " + state.getGold());
                livesLabel.setText("Lives: " + state.getLives());
                waveLabel.setText("Wave: " + state.getWave());
                if (state.isGameOver()) {
                    showMessage("Game over! You survived " + (state.getWave() - 1) + " waves.", true);
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
        messageLabel.setTextFill(error ? Color.FIREBRICK : Color.DARKGREEN);
    }

    private void draw(GraphicsContext g) {
        // tiles and towers
        for (int r = 0; r < GameState.SIZE; r++) {
            for (int c = 0; c < GameState.SIZE; c++) {
                g.setFill(state.isPath(r, c) ? Color.BURLYWOOD : Color.YELLOWGREEN);
                g.fillRect(c * CELL, r * CELL, CELL, CELL);
                g.setStroke(Color.DARKOLIVEGREEN);
                g.strokeRect(c * CELL, r * CELL, CELL, CELL);

                Tower t = state.getTower(r, c);
                if (t != null) {
                    drawTower(g, t);
                }
            }
        }

        // range circle for the selected tower
        if (selectedRow >= 0) {
            Tower t = state.getTower(selectedRow, selectedCol);
            double radius = t.getRange() * CELL;
            double cx = selectedCol * CELL + CELL / 2.0;
            double cy = selectedRow * CELL + CELL / 2.0;
            g.setStroke(Color.WHITE);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }

        // enemies
        for (Enemy e : state.getEnemies()) {
            drawEnemy(g, e);
        }
    }

    private void drawTower(GraphicsContext g, Tower t) {
        Color color;
        switch (t.getName()) {
            case "Archer": color = Color.SADDLEBROWN; break;
            case "Cannon": color = Color.DIMGRAY; break;
            default:       color = Color.MEDIUMPURPLE; break;
        }
        double x = t.getCol() * CELL + 15;
        double y = t.getRow() * CELL + 15;
        g.setFill(color);
        g.fillRect(x, y, CELL - 30, CELL - 30);
        g.setFill(Color.WHITE);
        g.fillText(t.getName().substring(0, 1), x + 20, y + 30);
        // one gold dot per upgrade level
        g.setFill(Color.GOLD);
        for (int i = 0; i < t.getLevel(); i++) {
            g.fillOval(x + 4 + i * 12, y - 10, 8, 8);
        }
    }

    private void drawEnemy(GraphicsContext g, Enemy e) {
        Color color;
        double size;
        switch (e.getName()) {
            case "Goblin": color = Color.FORESTGREEN; size = 20; break;
            case "Orc":    color = Color.DARKRED; size = 28; break;
            case "Slime":  color = Color.DEEPSKYBLUE; size = 24; break;
            default:       color = Color.BLACK; size = 40; break;
        }
        double cx = e.getX() * CELL + CELL / 2.0;
        double cy = e.getY() * CELL + CELL / 2.0;
        g.setFill(e.isSlowed() ? color.interpolate(Color.LIGHTBLUE, 0.5) : color);
        g.fillOval(cx - size / 2, cy - size / 2, size, size);

        // health bar
        double hpWidth = 30.0 * e.getHealth() / e.getMaxHealth();
        g.setFill(Color.RED);
        g.fillRect(cx - 15, cy - size / 2 - 8, 30, 4);
        g.setFill(Color.LIME);
        g.fillRect(cx - 15, cy - size / 2 - 8, hpWidth, 4);
    }
}
