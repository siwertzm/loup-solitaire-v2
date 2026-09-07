package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.EmailVerificationToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.EmailVerificationTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationHours", 24L);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");

        utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setEmailVerifie(false);
    }

    @Test
    void envoyerLienDeVerificationPersisteUnTokenEtEnvoieUnEmailAvecLeLien() {
        service.envoyerLienDeVerification(utilisateur);

        verify(tokenRepository).save(org.mockito.ArgumentMatchers.any(EmailVerificationToken.class));
        verify(emailService).envoyerEmailVerification(
                eq("marius@example.com"),
                contains("http://localhost:8080/auth/verify-email?token=")
        );
    }

    @Test
    void verifierActiveLeCompteQuandLeTokenEstValide() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setUtilise(false);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.verifier("token-valide");

        assertThat(utilisateur.isEmailVerifie()).isTrue();
        assertThat(token.isUtilise()).isTrue();
        verify(utilisateurRepository).save(utilisateur);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifierRejetteUnTokenDejaUtilise() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setUtilise(true);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifier("token-deja-utilise"))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("deja ete utilise");

        assertThat(utilisateur.isEmailVerifie()).isFalse();
        verify(utilisateurRepository, never()).save(utilisateur);
    }

    @Test
    void verifierRejetteUnTokenExpire() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setUtilise(false);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifier("token-expire"))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("expire");

        assertThat(utilisateur.isEmailVerifie()).isFalse();
    }

    @Test
    void verifierRejetteUnTokenInconnu() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifier("token-inconnu"))
                .isInstanceOf(TokenInvalideException.class);
    }
}