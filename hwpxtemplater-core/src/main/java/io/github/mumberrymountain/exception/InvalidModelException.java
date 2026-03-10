package io.github.mumberrymountain.exception;

public class InvalidModelException extends RuntimeException {
    public InvalidModelException(String msg) {
        super(msg);
    }

    public InvalidModelException(String msg, Throwable e) {
        super(msg, e);
    }
}
