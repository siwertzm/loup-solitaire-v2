package com.loupsolitaire.backend.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Proprietes "app.limitation-debit.*" (SEC-02), verifiees au demarrage.
 *
 * @param active             false pour tout desactiver (tests de charge, debug)
 * @param enteteIpClient     en-tete pose par l'hebergeur avec l'IP reelle du
 *                           client (ex. "True-Client-IP") ; vide = utiliser
 *                           X-Forwarded-For
 * @param proxiesDeConfiance nombre de proxys de confiance devant l'API, qui
 *                           ajoutent chacun une adresse a droite de
 *                           X-Forwarded-For ; 0 = ignorer l'en-tete et
 *                           prendre l'adresse de la connexion
 * @param regles             regles par nom (voir LimiteurDeDebit.REGLES)
 */
@Validated
@ConfigurationProperties(prefix = "app.limitation-debit")
public record LimitationDebitProperties(
        @DefaultValue("true") boolean active,
        @DefaultValue("") String enteteIpClient,
        @DefaultValue("1") @PositiveOrZero int proxiesDeConfiance,
        @NotNull Map<String, @Valid Regle> regles) {

    /**
     * Au plus {@code limite} evenements par {@code fenetre}, par cle (IP,
     * email ou identifiant selon la regle).
     */
    public record Regle(@Positive int limite, @NotNull Duration fenetre) {
    }
}