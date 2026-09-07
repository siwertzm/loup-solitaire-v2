package com.loupsolitaire.backend.response;

public record AuthResponse(String accessToken, String refreshToken, String tokenType) {

    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
