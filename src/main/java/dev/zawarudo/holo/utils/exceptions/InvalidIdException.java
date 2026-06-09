package dev.zawarudo.holo.utils.exceptions;

/**
 * An exception that is thrown when the given id is not a valid id.
 */
public class InvalidIdException extends Exception {

    public InvalidIdException() {
    }

    public InvalidIdException(String message) {
        super(message);
    }

    public InvalidIdException(String message, Throwable e) {
        super(message, e);
    }

    public InvalidIdException(Throwable e) {
        super(e);
    }
}
