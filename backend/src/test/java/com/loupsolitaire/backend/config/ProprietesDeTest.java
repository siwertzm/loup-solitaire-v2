package com.loupsolitaire.backend.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    // Memes regles que application.properties (SEC-02).
    public static LimitationDebitProperties limitationDebit() {
        return new LimitationDebitProperties(true, "", 1, Map.of(
                "auth-ip", regle(60, Duration.ofMinutes(1)),
                "login-ip", regle(10, Duration.ofMinutes(1)),
                "inscription-ip", regle(5, Duration.ofHours(1)),
                "email-ip", regle(10, Duration.ofHours(1)),
                "refresh-ip", regle(30, Duration.ofMinutes(1)),
                "login-compte", regle(10, Duration.ofMinutes(15)),
                "reset-demande-email", regle(3, Duration.ofHours(1)),
                "reset-code-email", regle(10, Duration.ofHours(24)),
                "renvoi-verification-email", regle(3, Duration.ofHours(1))));
    }

    private static LimitationDebitProperties.Regle regle(int limite, Duration fenetre) {
        return new LimitationDebitProperties.Regle(limite, fenetre);
    }

    public static JwtProperties jwt() {
        return jwt(SECRET_JWT, 3_600_000L);
    }

    public static JwtProperties jwt(String secret, long expirationMs) {
        return new JwtProperties(secret, expirationMs, 30);
    }
}