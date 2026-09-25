package com.loupsolitaire.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Proprietes "app.*" de application.properties, regroupees et verifiees au
 * DEMARRAGE (remplace les @Value disperses dans les services) : une valeur
 * manquante ou invalide empeche l'application de demarrer.
 *
 * Correspondance avec application.properties (noms en kebab-case) :
 * app.base-url, app.mobile-login-url, app.cors.allowed-origins,
 * app.mail.from, app.email-verification.expiration-hours,
 * app.password-reset.expiration-minutes, app.password-reset.max-attempts.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        // Base des liens envoyes par email (lien de confirmation).
        @NotBlank String baseUrl,
        // Deep link ouvert apres la confirmation de l'email.
        @NotBlank String mobileLoginUrl,
        @NotNull @Valid Cors cors,
        @NotNull @Valid Mail mail,
        @NotNull @Valid EmailVerification emailVerification,
        @Valid @DefaultValue PasswordReset passwordReset) {

    // Origines autorisees a appeler l'API (CORS_ALLOWED_ORIGINS, separees
    // par des virgules).
    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    // Adresse d'expedition des emails.
    public record Mail(@NotBlank String from) {
    }

    public record EmailVerification(@Positive long expirationHours) {
    }

    public record PasswordReset(
            @DefaultValue("15") @Positive long expirationMinutes,
            @DefaultValue("5") @Positive int maxAttempts) {
    }
}