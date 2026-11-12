package td.exception;

/**
 * Thrown when the commander cannot afford a build or upgrade.
 */
public class InsufficientGoldException extends Exception {

    public InsufficientGoldException(String message) {
        super(message);
    }
}
