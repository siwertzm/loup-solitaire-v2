package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.loupsolitaire.backend.service.PurgeService;

/**
 * OPS-05, sur PostgreSQL : la purge supprime les jetons qui ne servent plus,
 * garde ceux qui servent encore, et ne supprime jamais de compte.
 */
class PurgeIT extends IntegrationPostgres {

    private static final Instant MAINTENANT = Instant.parse("2026-11-12T04:00:00Z");

    @Autowired
    private PurgeService purgeService;

    private final UUID utilisateurId = UUID.randomUUID();

    @BeforeEach
    void unCompteNonVerifieAncienAvecDesJetons() {
        jdbc.update("""
                insert into utilisateur (id, date_creation, email, email_verifie, password, username)
                values (?, ?, ?, false, 'x', ?)
                """, utilisateurId, Timestamp.from(MAINTENANT.minus(Duration.ofDays(30))),
                utilisateurId + "@test.local", "purge-" + utilisateurId);
    }

    @AfterEach
    void nettoyer() {
        for (String table : new String[] {"refresh_token", "email_verification_token", "password_reset_token"}) {
            jdbc.update("delete from " + table + " where utilisateur_id = ?", utilisateurId);
        }
        jdbc.update("delete from utilisateur where id = ?", utilisateurId);
    }

    private static Timestamp il_y_a(Duration duree) {
        return Timestamp.from(MAINTENANT.minus(duree));
    }

    private static Timestamp dans(Duration duree) {
        return Timestamp.from(MAINTENANT.plus(duree));
    }

    private UUID session(Timestamp expiration, boolean revoquee) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into refresh_token (id, created_at, expires_at, revoked, token_hash, utilisateur_id)
                values (?, ?, ?, ?, ?, ?)
                """, id, il_y_a(Duration.ofDays(40)), expiration, revoquee, id.toString(), utilisateurId);
        return id;
    }

    private UUID lienDeVerification(Timestamp expiration) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into email_verification_token (id, created_at, expires_at, token_hash, utilise, utilisateur_id)
                values (?, ?, ?, ?, false, ?)
                """, id, il_y_a(Duration.ofDays(2)), expiration, id.toString(), utilisateurId);
        return id;
    }

    private UUID demandeMotDePasse(Timestamp expirationCode, Timestamp expirationReinitialisation) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into password_reset_token (id, code_hash, created_at, expires_at, tentatives, utilise,
                    utilisateur_id, reset_token_expires_at)
                values (?, 'x', ?, ?, 0, false, ?, ?)
                """, id, il_y_a(Duration.ofHours(1)), expirationCode, utilisateurId, expirationReinitialisation);
        return id;
    }

    private boolean existe(String table, UUID id) {
        return jdbc.queryForObject("select count(*) from " + table + " where id = ?", Long.class, id) == 1;
    }

    @Test
    void supprimeLesSessionsExpireesDepuisPlusDeSeptJoursEtGardeLesAutres() {
        UUID expireeIlYAHuitJours = session(il_y_a(Duration.ofDays(8)), false);
        UUID expireeHier = session(il_y_a(Duration.ofDays(1)), false);
        // SEC-04 : revoquee mais pas expiree, sert a detecter la reutilisation.
        UUID revoqueeNonExpiree = session(dans(Duration.ofDays(20)), true);

        PurgeService.Resultat resultat = purgeService.purger(MAINTENANT);

        assertThat(existe("refresh_token", expireeIlYAHuitJours)).isFalse();
        assertThat(existe("refresh_token", expireeHier)).isTrue();
        assertThat(existe("refresh_token", revoqueeNonExpiree)).isTrue();
        assertThat(resultat.sessions()).isPositive();
    }

    @Test
    void supprimeLesLiensDeVerificationExpires() {
        UUID expire = lienDeVerification(il_y_a(Duration.ofHours(1)));
        UUID valide = lienDeVerification(dans(Duration.ofHours(10)));

        purgeService.purger(MAINTENANT);

        assertThat(existe("email_verification_token", expire)).isFalse();
        assertThat(existe("email_verification_token", valide)).isTrue();
    }

    @Test
    void supprimeLesDemandesDeMotDePasseDontToutAExpire() {
        UUID codeExpire = demandeMotDePasse(il_y_a(Duration.ofMinutes(5)), null);
        UUID reinitialisationExpiree = demandeMotDePasse(il_y_a(Duration.ofMinutes(30)), il_y_a(Duration.ofMinutes(1)));
        // Code expire, mais l'etape de reinitialisation est encore ouverte.
        UUID reinitialisationEnCours = demandeMotDePasse(il_y_a(Duration.ofMinutes(5)), dans(Duration.ofMinutes(10)));
        UUID codeValide = demandeMotDePasse(dans(Duration.ofMinutes(10)), null);

        purgeService.purger(MAINTENANT);

        assertThat(existe("password_reset_token", codeExpire)).isFalse();
        assertThat(existe("password_reset_token", reinitialisationExpiree)).isFalse();
        assertThat(existe("password_reset_token", reinitialisationEnCours)).isTrue();
        assertThat(existe("password_reset_token", codeValide)).isTrue();
    }

    @Test
    void neSupprimeJamaisDeCompteMemeNonVerifieEtAncien() {
        lienDeVerification(il_y_a(Duration.ofDays(29)));

        purgeService.purger(MAINTENANT);

        assertThat(existe("utilisateur", utilisateurId)).isTrue();
    }
}
