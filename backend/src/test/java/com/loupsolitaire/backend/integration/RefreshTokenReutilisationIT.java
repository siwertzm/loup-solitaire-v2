package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.service.AuthService;
import com.loupsolitaire.backend.service.RefreshTokenService;

/**
 * SEC-04 sur PostgreSQL : un refresh token vole puis rejoue revoque TOUTES
 * les sessions du joueur, et cette revocation reste en base alors que la
 * requete de l'attaquant echoue (transaction REQUIRES_NEW). Verifie en SQL,
 * directement dans la table.
 */
class RefreshTokenReutilisationIT extends IntegrationPostgres {

    private static final String SESSIONS_ACTIVES =
            "select count(*) from refresh_token where revoked = false and utilisateur_id = ?";

    @Autowired
    private AuthService authService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Utilisateur utilisateur;

    @BeforeEach
    void creerLeJoueur() {
        utilisateur = new TransactionTemplate(transactionManager).execute(status -> {
            Utilisateur u = new Utilisateur();
            u.setUsername("sec04-" + UUID.randomUUID());
            u.setEmail(UUID.randomUUID() + "@example.com");
            u.setPassword("hash");
            u.setEmailVerifie(true);
            u.setDateCreation(Instant.now());
            return utilisateurRepository.save(u);
        });
    }

    @AfterEach
    void supprimerLeJoueur() {
        jdbc.update("delete from refresh_token where utilisateur_id = ?", utilisateur.getId());
        jdbc.update("delete from utilisateur where id = ?", utilisateur.getId());
    }

    private long sessionsActives() {
        return jdbc.queryForObject(SESSIONS_ACTIVES, Long.class, utilisateur.getId());
    }

    @Test
    void rejouerUnTokenDejaUtiliseRevoqueToutesLesSessionsEnBase() {
        String autreAppareil = refreshTokenService.creerToken(utilisateur);
        String jetonVole = refreshTokenService.creerToken(utilisateur);

        // Le joueur rafraichit en premier : rotation normale.
        AuthResponse joueur = authService.rafraichir(jetonVole);
        assertThat(sessionsActives()).isEqualTo(2);

        // L'attaquant rejoue l'ancien token : refuse...
        assertThatThrownBy(() -> authService.rafraichir(jetonVole))
                .isInstanceOf(TokenInvalideException.class);

        // ... et plus aucune session active, malgre l'exception.
        assertThat(sessionsActives()).isZero();
        assertThatThrownBy(() -> authService.rafraichir(autreAppareil))
                .isInstanceOf(TokenInvalideException.class);
        assertThatThrownBy(() -> authService.rafraichir(joueur.refreshToken()))
                .isInstanceOf(TokenInvalideException.class);
    }
}