package com.loupsolitaire.backend.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(Instant timestamp, int status, String error, Object message) {

    public static ErrorResponse of(int status, String error, Object message) {
        return new ErrorResponse(Instant.now(), status, error, message);
    }

    public static ErrorResponse of(int status, String error, String field, String message) {
        return of(status, error, Map.of(field, message));
    }
}
