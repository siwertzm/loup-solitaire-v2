package com.loupsolitaire.backend.config;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Proprietes "jwt.*" de application.properties, verifiees au DEMARRAGE :
 * une valeur manquante ou invalide empeche l'application de demarrer, au lieu
 * d'une erreur au premier login.
 *
 * @param secret                cle HMAC en Base64 (JWT_SECRET), au moins 256 bits
 * @param expirationMs          duree de vie de l'access token
 * @param refreshExpirationDays duree de vie du refresh token
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long expirationMs,
        @Positive long refreshExpirationDays) {

    private static final int OCTETS_MINIMUM = 32;

    public JwtProperties {
        // Une valeur vide est signalee par @NotBlank ci-dessus.
        if (secret != null && !secret.isBlank()) {
            byte[] cle;
            try {
                cle = Decoders.BASE64.decode(secret);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "jwt.secret (JWT_SECRET) n'est pas du Base64 valide (generer avec : openssl rand -base64 32)", e);
            }
            // HS256 exige une cle d'au moins 256 bits (32 octets).
            if (cle.length < OCTETS_MINIMUM) {
                throw new IllegalArgumentException(
                        "jwt.secret (JWT_SECRET) doit faire au moins 32 octets une fois decode "
                                + "(generer avec : openssl rand -base64 32)");
            }
        }
    }

    // Cle de signature, calculee une seule fois (voir JwtUtil).
    public SecretKey cleDeSignature() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}