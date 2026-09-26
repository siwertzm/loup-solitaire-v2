package com.loupsolitaire.backend.config;

import java.util.Set;

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
        @NotBlank(message = "JWT_SECRET est obligatoire (generer avec : openssl rand -base64 32)") String secret,
        @Positive long expirationMs,
        @Positive long refreshExpirationDays) {

    private static final int OCTETS_MINIMUM = 32;

    // SEC-03 : cles deja publiees dans le depot (anciennes valeurs par
    // defaut de application.properties). N'importe qui peut forger un jeton
    // avec : refusees meme si elles sont fournies explicitement.
    private static final Set<String> CLES_PUBLIEES = Set.of(
            "bG91cC1zb2xpdGFpcmUtREVWLU9OTFktc2VjcmV0LW5ldmVyLXVzZS1pbi1wcm9k");

    public JwtProperties {
        // Une valeur vide est signalee par @NotBlank ci-dessus.
        if (secret != null && !secret.isBlank()) {
            if (CLES_PUBLIEES.contains(secret.trim())) {
                throw new IllegalArgumentException(
                        "jwt.secret (JWT_SECRET) est une cle publiee dans le depot : "
                                + "en generer une nouvelle avec : openssl rand -base64 32");
            }
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