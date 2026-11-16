package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    // SEC-11 : deux rafraichissements SIMULTANES du meme jeton. Le premier
    // est une transaction pas encore validee ; le second doit attendre son
    // verrou, puis voir le jeton deja utilise et revoquer toutes les
    // sessions. Sans verrou, il lisait "pas encore utilise" et passait aussi.
    @Test
    void deuxRafraichissementsSimultanesDuMemeJetonNeDonnentQuUneSession() throws Exception {
        String jeton = refreshTokenService.creerToken(utilisateur);
        CountDownLatch premierFait = new CountDownLatch(1);
        CountDownLatch validerPremier = new CountDownLatch(1);

        CompletableFuture<Void> premier = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    authService.rafraichir(jeton);
                    premierFait.countDown();
                    attendre(validerPremier);
                }));
        assertThat(premierFait.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> {
            try {
                authService.rafraichir(jeton);
                return "ACCEPTE";
            } catch (TokenInvalideException e) {
                return "REFUSE";
            }
        });

        // Le second attend le verrou du premier, encore ouvert.
        assertThatThrownBy(() -> second.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

        validerPremier.countDown();
        premier.get(10, TimeUnit.SECONDS);
        assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("REFUSE");
        assertThat(sessionsActives()).isZero();
    }

    private static void attendre(CountDownLatch signal) {
        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
