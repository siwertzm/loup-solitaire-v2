package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.RefreshTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.AuthResponse;

/**
 * SEC-04, avec une vraie base (H2) et les vraies transactions Spring : un
 * mock ne peut pas montrer qu'une exception annule (ou non) ce qui a ete
 * ecrit avant elle.
 *
 * Scenario : un attaquant a vole un refresh token. Le joueur l'utilise en
 * premier (rotation normale). L'attaquant presente ensuite le meme token,
 * deja revoque : la requete est refusee ET toutes les sessions du joueur
 * doivent rester revoquees en base, y compris celle de son autre appareil.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.datasource.hikari.connection-init-sql="
})
class RevocationApresReutilisationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        utilisateur = transaction.execute(status -> {
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
    void tearDown() {
        transaction.executeWithoutResult(status -> {
            refreshTokenRepository.deleteByUtilisateur(utilisateur);
            utilisateurRepository.deleteById(utilisateur.getId());
        });
    }

    private long sessionsActives() {
        return transaction.execute(status ->
                (long) refreshTokenRepository.findAllByUtilisateurAndRevokedFalse(utilisateur).size());
    }

    @Test
    void laRevocationSurvitAuRefusDeLaRequete() {
        String autreAppareil = refreshTokenService.creerToken(utilisateur);
        String jetonVole = refreshTokenService.creerToken(utilisateur);

        // Le joueur rafraichit en premier : rotation normale.
        AuthResponse joueur = authService.rafraichir(jetonVole);
        assertThat(joueur.refreshToken()).isNotBlank();
        // autreAppareil + le nouveau token du joueur
        assertThat(sessionsActives()).isEqualTo(2);

        // L'attaquant rejoue le token deja utilise.
        assertThatThrownBy(() -> authService.rafraichir(jetonVole))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("Reutilisation");

        // Critere SEC-04 : plus aucune session active, malgre l'exception.
        assertThat(sessionsActives()).isZero();
        assertThatThrownBy(() -> authService.rafraichir(autreAppareil))
                .isInstanceOf(TokenInvalideException.class);
        assertThatThrownBy(() -> authService.rafraichir(joueur.refreshToken()))
                .isInstanceOf(TokenInvalideException.class);
    }

    @Test
    void unTokenExpireOuInconnuNeRevoquePasLesAutresSessions() {
        refreshTokenService.creerToken(utilisateur);

        assertThatThrownBy(() -> authService.rafraichir("token-qui-n-existe-pas"))
                .isInstanceOf(TokenInvalideException.class);

        assertThat(sessionsActives()).isEqualTo(1);
    }
}