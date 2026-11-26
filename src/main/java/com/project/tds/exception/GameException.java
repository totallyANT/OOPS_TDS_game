package com.project.tds.exception;

/**
 * Base checked exception for all domain-level game rule violations.
 */
public class GameException extends Exception {
    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
