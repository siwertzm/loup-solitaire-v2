package com.loupsolitaire.backend.config;

import java.util.List;

/**
 * Valeurs de configuration pour les tests unitaires (sans contexte Spring),
 * identiques aux valeurs par defaut de application.properties.
 */
public final class ProprietesDeTest {

    // Cle de test (Base64, 39 octets), jamais utilisee en dehors des tests.
    public static final String SECRET_JWT = "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi";

    private ProprietesDeTest() {
    }

    public static AppProperties app() {
        return new AppProperties(
                "http://localhost:8080",
                "loupsolitaire://auth/login?emailVerified=true",
                new AppProperties.Cors(List.of("http://localhost:4200")),
                new AppProperties.Mail("no-reply@loup-solitaire.local"),
                new AppProperties.EmailVerification(24),
                new AppProperties.PasswordReset(15, 5));
    }

    public static JwtProperties jwt() {
        return jwt(SECRET_JWT, 3_600_000L);
    }

    public static JwtProperties jwt(String secret, long expirationMs) {
        return new JwtProperties(secret, expirationMs, 30);
    }
}