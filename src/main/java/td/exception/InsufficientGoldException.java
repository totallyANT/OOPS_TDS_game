package td.exception;

/**
 * Thrown when a player attempts an action (such as building or upgrading a tower)
 * without having enough gold.
 */
public class InsufficientGoldException extends Exception {
    public InsufficientGoldException(String message) {
        super(message);
    }
}
