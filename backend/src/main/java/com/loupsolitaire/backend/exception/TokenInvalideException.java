package com.loupsolitaire.backend.exception;

public class TokenInvalideException extends RuntimeException {
    public TokenInvalideException(String message) {
        super(message);
    }
}
