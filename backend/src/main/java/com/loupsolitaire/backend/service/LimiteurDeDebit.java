package com.loupsolitaire.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loupsolitaire.backend.config.LimitationDebitProperties;
import com.loupsolitaire.backend.config.LimitationDebitProperties.Regle;
import com.loupsolitaire.backend.exception.TropDeRequetesException;

/**
 * Limitation de debit (SEC-02) : au plus N evenements par fenetre de temps
 * fixe, par regle et par cle (une IP, un email, un identifiant).
 *
 * Compteurs en memoire (Caffeine) : suffisant pour UNE instance du backend,
 * comme aujourd'hui sur Render. Avec plusieurs instances, chaque instance
 * compterait de son cote : passer alors a un stockage partage (Redis ou
 * PostgreSQL).
 *
 * Deux usages :
 * - consommer() compte chaque appel (limite par IP, demandes d'email) ;
 * - verifier() + enregistrerEchec() ne comptent que les ECHECS (mauvais mot
 *   de passe, mauvais code), et reinitialiser() remet a zero apres un succes.
 */
@Service
public class LimiteurDeDebit {

    // Par IP (LimitationDebitFilter).
    public static final String AUTH_IP = "auth-ip";
    public static final String LOGIN_IP = "login-ip";
    public static final String INSCRIPTION_IP = "inscription-ip";
    public static final String EMAIL_IP = "email-ip";
    public static final String REFRESH_IP = "refresh-ip";
    // Par compte ou par email (services).
    public static final String LOGIN_COMPTE = "login-compte";
    public static final String RESET_DEMANDE_EMAIL = "reset-demande-email";
    public static final String RESET_CODE_EMAIL = "reset-code-email";
    public static final String RENVOI_VERIFICATION_EMAIL = "renvoi-verification-email";

    // Toutes les regles utilisees par le code : chacune doit etre configuree
    // (application.properties), sinon l'application refuse de demarrer.
    public static final Set<String> REGLES = Set.of(
            AUTH_IP, LOGIN_IP, INSCRIPTION_IP, EMAIL_IP, REFRESH_IP,
            LOGIN_COMPTE, RESET_DEMANDE_EMAIL, RESET_CODE_EMAIL, RENVOI_VERIFICATION_EMAIL);

    // Borne la memoire : au-dela, les compteurs les moins utilises sont
    // oublies (un attaquant qui change d'IP sans arret ne fait pas grossir
    // le tas indefiniment).
    private static final long NOMBRE_MAX_DE_COMPTEURS = 200_000;

    private final LimitationDebitProperties proprietes;
    private final Clock horloge;
    private final Cache<String, Fenetre> compteurs;

    @Autowired
    public LimiteurDeDebit(LimitationDebitProperties proprietes) {
        this(proprietes, Clock.systemUTC());
    }

    LimiteurDeDebit(LimitationDebitProperties proprietes, Clock horloge) {
        this.proprietes = proprietes;
        this.horloge = horloge;

        for (String regle : REGLES) {
            if (!proprietes.regles().containsKey(regle)) {
                throw new IllegalStateException(
                        "Regle de limitation de debit manquante : app.limitation-debit.regles." + regle);
            }
        }
        Duration fenetreMax = proprietes.regles().values().stream()
                .map(Regle::fenetre)
                .max(Duration::compareTo)
                .orElse(Duration.ofDays(1));

        this.compteurs = Caffeine.newBuilder()
                .maximumSize(NOMBRE_MAX_DE_COMPTEURS)
                .expireAfterWrite(fenetreMax)
                .build();
    }

    /** Compte un evenement ; 429 si la limite de la fenetre est deja atteinte. */
    public void consommer(String regle, String cle) {
        verifier(regle, cle);
        enregistrerEchec(regle, cle);
    }

    /** 429 si la limite est atteinte, sans compter cet appel. */
    public void verifier(String regle, String cle) {
        if (!proprietes.active()) {
            return;
        }
        Regle config = config(regle);
        Instant maintenant = horloge.instant();
        Fenetre fenetre = compteurs.getIfPresent(cleComplete(regle, cle));
        if (fenetre != null && fenetre.enCours(maintenant, config) && fenetre.nombre() >= config.limite()) {
            throw new TropDeRequetesException(fenetre.secondesRestantes(maintenant, config));
        }
    }

    /** Compte un evenement (typiquement un echec), sans verifier la limite. */
    public void enregistrerEchec(String regle, String cle) {
        if (!proprietes.active()) {
            return;
        }
        Regle config = config(regle);
        Instant maintenant = horloge.instant();
        compteurs.asMap().compute(cleComplete(regle, cle), (k, fenetre) ->
                fenetre != null && fenetre.enCours(maintenant, config)
                        ? new Fenetre(fenetre.debut(), fenetre.nombre() + 1)
                        : new Fenetre(maintenant, 1));
    }

    /** Remet le compteur a zero (ex. connexion reussie). */
    public void reinitialiser(String regle, String cle) {
        compteurs.invalidate(cleComplete(regle, cle));
    }

    private Regle config(String regle) {
        Regle config = proprietes.regles().get(regle);
        if (config == null) {
            throw new IllegalArgumentException("Regle de limitation de debit inconnue : " + regle);
        }
        return config;
    }

    // Emails et identifiants : insensibles a la casse et aux espaces, pour
    // qu'une variante d'ecriture ne donne pas un nouveau compteur.
    private static String cleComplete(String regle, String cle) {
        String normalisee = cle == null ? "" : cle.trim().toLowerCase(Locale.ROOT);
        return regle + '|' + normalisee;
    }

    private record Fenetre(Instant debut, int nombre) {

        boolean enCours(Instant maintenant, Regle config) {
            return maintenant.isBefore(debut.plus(config.fenetre()));
        }

        long secondesRestantes(Instant maintenant, Regle config) {
            long secondes = Duration.between(maintenant, debut.plus(config.fenetre())).toSeconds();
            return Math.max(1, secondes);
        }
    }
}