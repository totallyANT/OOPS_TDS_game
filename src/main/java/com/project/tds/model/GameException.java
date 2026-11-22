package com.project.tds.model;

/** Thrown when the player tries something the rules don't allow (bad tile, not enough gold, etc.). */
public class GameException extends Exception {
    public GameException(String message) {
        super(message);
    }
}
