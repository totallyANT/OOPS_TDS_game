package td.exception;

/**
 * Thrown when an invalid grid placement or action is attempted, such as
 * placing on the enemy path, placing on an occupied cell, or performing an action
 * while an active wave is in progress.
 */
public class InvalidPlacementException extends Exception {
    public InvalidPlacementException(String message) {
        super(message);
    }
}
