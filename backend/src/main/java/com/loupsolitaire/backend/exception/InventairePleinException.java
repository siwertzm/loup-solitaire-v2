package com.loupsolitaire.backend.exception;

public class InventairePleinException extends RuntimeException {
    public InventairePleinException(String message) {
        super(message);
    }
}