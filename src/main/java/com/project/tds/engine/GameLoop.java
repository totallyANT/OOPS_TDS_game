package com.project.tds.engine;

/** Runs the game logic on its own thread, about 60 times a second. */
public class GameLoop implements Runnable {
    private final GameState state;
    private volatile boolean running = true;

    public GameLoop(GameState state) {
        this.state = state;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            state.update((now - last) / 1_000_000_000.0);
            last = now;
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
