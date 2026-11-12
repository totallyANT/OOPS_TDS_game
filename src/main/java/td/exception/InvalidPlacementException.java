package td.exception;

/**
 * Thrown for illegal battlefield actions such as building on the path,
 * building on an occupied tile, acting during a wave, or using a blank name.
 */
public class InvalidPlacementException extends Exception {

    public InvalidPlacementException(String message) {
        super(message);
    }
}
