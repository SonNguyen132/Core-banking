package com.finaegis.eventstore;

/**
 * Thrown when an aggregate is modified concurrently (optimistic locking failure).
 */
public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException(String message) {
        super(message);
    }
}
