package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.loupsolitaire.backend.config.ProprietesDeTest;
import com.loupsolitaire.backend.exception.TropDeRequetesException;
import com.loupsolitaire.backend.model.PasswordResetToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PasswordResetTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.util.Tokens;

/**
 * Parcours complet "mot de passe oublie", en 3 etapes :
 * 1. demanderReinitialisation : code a 6 chiffres envoye par email ;
 * 2. verifierCode : le code donne un token de reinitialisation ;
 * 3. reinitialiser : le token permet de choisir le nouveau mot de passe.
 *
 * Configuration de test : code valable 15 minutes, 5 essais maximum.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "marius@example.com";
    private static final String MESSAGE_CODE = "Code invalide ou expire";
    private static final String MESSAGE_REINITIALISATION = "Reinitialisation invalide ou expiree";

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordResetService service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        // Vrai limiteur (regles de application.properties) : les tests SEC-02
        // ci-dessous verifient le comportement de bout en bout.
        service = new PasswordResetService(utilisateurRepository, tokenRepository, passwordEncoder, emailService,
                refreshTokenService, ProprietesDeTest.app(), new LimiteurDeDebit(ProprietesDeTest.limitationDebit()));

        utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setEmail(EMAIL);
        utilisateur.setPassword("ancien-hash");
    }

    private PasswordResetToken token(int tentatives, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUtilisateur(utilisateur);
        token.setCodeHash("hash-du-code");
        token.setTentatives(tentatives);
        token.setExpiresAt(expiresAt);
        return token;
    }

    private Instant dansDixMinutes() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private void stuberDernierCode(PasswordResetToken token) {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(tokenRepository.findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(utilisateur))
                .thenReturn(Optional.of(token));
    }

    // =========================================================
    // Etape 1 : demanderReinitialisation
    // =========================================================

    @Test
    void demanderEnvoieUnCodeASixChiffresEtNeGardeQueSonHash() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(tokenRepository.findByUtilisateurAndUtiliseFalse(utilisateur)).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-du-code");

        service.demanderReinitialisation(EMAIL);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyerCodeReinitialisationMotDePasse(eq(EMAIL), code.capture(), eq(15L));
        assertThat(code.getValue()).matches("\\d{6}");
        // Le code brut est hache (BCrypt), jamais stocke tel quel.
        verify(passwordEncoder).encode(code.getValue());

        ArgumentCaptor<PasswordResetToken> enregistre = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(enregistre.capture());
        PasswordResetToken token = enregistre.getValue();
        assertThat(token.getUtilisateur()).isSameAs(utilisateur);
        assertThat(token.getCodeHash()).isEqualTo("hash-du-code");
        assertThat(token.getExpiresAt())
                .isBetween(Instant.now().plus(14, ChronoUnit.MINUTES), Instant.now().plus(16, ChronoUnit.MINUTES));
    }

    @Test
    void demanderInvalideLesCodesPrecedents() {
        PasswordResetToken ancien = token(0, dansDixMinutes());
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(tokenRepository.findByUtilisateurAndUtiliseFalse(utilisateur)).thenReturn(List.of(ancien));
        when(passwordEncoder.encode(anyString())).thenReturn("nouveau-hash");

        service.demanderReinitialisation(EMAIL);

        assertThat(ancien.isUtilise()).isTrue();
    }

    @Test
    void demanderNeFaitRienPourUnEmailInconnuSansLeReveler() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Pas d'erreur : l'appelant ne doit pas pouvoir deviner si l'email existe.
        service.demanderReinitialisation(EMAIL);

        verifyNoInteractions(tokenRepository, emailService, passwordEncoder);
    }

    // =========================================================
    // Etape 2 : verifierCode
    // =========================================================

    @Test
    void unBonCodeDonneUnTokenDeReinitialisationDontSeulLeHashEstGarde() {
        PasswordResetToken token = token(0, dansDixMinutes());
        stuberDernierCode(token);
        when(passwordEncoder.matches("123456", "hash-du-code")).thenReturn(true);

        String resetToken = service.verifierCode(EMAIL, "123456");

        assertThat(resetToken).isNotBlank();
        assertThat(token.getResetTokenHash()).isEqualTo(Tokens.hacher(resetToken));
        assertThat(token.getResetTokenExpiresAt()).isAfter(Instant.now());
        // Le code reste ouvert : il ne sera consomme qu'a l'etape 3.
        assertThat(token.isUtilise()).isFalse();
        verify(tokenRepository).save(token);
    }

    @Test
    void unMauvaisCodeAjouteUneTentative() {
        PasswordResetToken token = token(1, dansDixMinutes());
        stuberDernierCode(token);
        when(passwordEncoder.matches("000000", "hash-du-code")).thenReturn(false);

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);

        assertThat(token.getTentatives()).isEqualTo(2);
        assertThat(token.isUtilise()).isFalse();
        assertThat(token.getResetTokenHash()).isNull();
    }

    @Test
    void leCinquiemeMauvaisCodeInvalideLeCode() {
        PasswordResetToken token = token(4, dansDixMinutes());
        stuberDernierCode(token);
        when(passwordEncoder.matches("000000", "hash-du-code")).thenReturn(false);

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "000000"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(token.getTentatives()).isEqualTo(5);
        assertThat(token.isUtilise()).isTrue();
    }

    @Test
    void unCodeDontLesEssaisSontEpuisesEstRefuseSansLeComparer() {
        PasswordResetToken token = token(5, dansDixMinutes());
        stuberDernierCode(token);

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);

        // Meme le bon code ne passe plus : pas de comparaison du tout.
        verify(passwordEncoder, never()).matches(any(), any());
        assertThat(token.isUtilise()).isTrue();
    }

    @Test
    void unCodeExpireEstRefuseEtInvalide() {
        PasswordResetToken token = token(0, Instant.now().minus(1, ChronoUnit.MINUTES));
        stuberDernierCode(token);

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);

        verify(passwordEncoder, never()).matches(any(), any());
        assertThat(token.isUtilise()).isTrue();
    }

    @Test
    void unCodeDejaValideNePeutPasResservir() {
        PasswordResetToken token = token(0, dansDixMinutes());
        token.setResetTokenHash("deja-valide");
        stuberDernierCode(token);

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void verifierCodeAvecUnEmailInconnuDonneLeMemeMessage() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Meme message que pour un mauvais code : ne revele pas l'existence du compte.
        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);
    }

    @Test
    void verifierCodeSansDemandeEnCoursEstRefuse() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(tokenRepository.findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(utilisateur))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_CODE);
    }

    // =========================================================
    // Limitation de debit (SEC-02)
    // =========================================================

    @Test
    void auQuatriemeCodeDemandeEnUneHeureLaDemandeEstRefuseeEn429() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            service.demanderReinitialisation(EMAIL);
        }

        assertThatThrownBy(() -> service.demanderReinitialisation(EMAIL))
                .isInstanceOf(TropDeRequetesException.class);
        // Meme compteur quelle que soit l'ecriture de l'email.
        assertThatThrownBy(() -> service.demanderReinitialisation(" Marius@Example.com "))
                .isInstanceOf(TropDeRequetesException.class);
    }

    @Test
    void apresDixCodesFauxLEmailEstBloqueMemeAvecUnNouveauCode() {
        // Chaque essai porte sur un code neuf (0 tentative) : sans limite par
        // email, le compteur du token ne bloquerait jamais.
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(tokenRepository.findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(utilisateur))
                .thenAnswer(inv -> Optional.of(token(0, dansDixMinutes())));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> service.verifierCode(EMAIL, "000000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> service.verifierCode(EMAIL, "123456"))
                .isInstanceOf(TropDeRequetesException.class);
        verify(passwordEncoder, times(10)).matches(anyString(), anyString());
    }

    // =========================================================
    // Etape 3 : reinitialiser
    // =========================================================

    @Test
    void reinitialiserChangeLeMotDePasseEtDeconnecteToutesLesSessions() {
        PasswordResetToken token = token(0, dansDixMinutes());
        token.setResetTokenHash(Tokens.hacher("reset-token"));
        token.setResetTokenExpiresAt(dansDixMinutes());
        PasswordResetToken autreDemande = token(0, dansDixMinutes());

        when(tokenRepository.findByResetTokenHashAndUtiliseFalse(Tokens.hacher("reset-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nouveauMotDePasse")).thenReturn("nouveau-hash");
        when(tokenRepository.findByUtilisateurAndUtiliseFalse(utilisateur)).thenReturn(List.of(token, autreDemande));

        service.reinitialiser("reset-token", "nouveauMotDePasse");

        assertThat(utilisateur.getPassword()).isEqualTo("nouveau-hash");
        verify(utilisateurRepository).save(utilisateur);
        // Le token utilise et toutes les autres demandes ouvertes sont fermees.
        assertThat(token.isUtilise()).isTrue();
        assertThat(autreDemande.isUtilise()).isTrue();
        verify(refreshTokenService).revoquerToutesLesSessions(utilisateur);
    }

    @Test
    void reinitialiserAvecUnTokenInconnuEstRefuse() {
        when(tokenRepository.findByResetTokenHashAndUtiliseFalse(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reinitialiser("inconnu", "nouveauMotDePasse"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_REINITIALISATION);

        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void reinitialiserAvecUnTokenExpireEstRefuseEtLeFerme() {
        PasswordResetToken token = token(0, dansDixMinutes());
        token.setResetTokenHash(Tokens.hacher("reset-token"));
        token.setResetTokenExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByResetTokenHashAndUtiliseFalse(Tokens.hacher("reset-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reinitialiser("reset-token", "nouveauMotDePasse"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE_REINITIALISATION);

        assertThat(token.isUtilise()).isTrue();
        assertThat(utilisateur.getPassword()).isEqualTo("ancien-hash");
        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(refreshTokenService);
    }

    // =========================================================
    // Suppression de compte
    // =========================================================

    @Test
    void supprimerTokensSupprimeLesDemandesDeL_utilisateur() {
        service.supprimerTokens(utilisateur);

        verify(tokenRepository).deleteByUtilisateur(utilisateur);
    }
}