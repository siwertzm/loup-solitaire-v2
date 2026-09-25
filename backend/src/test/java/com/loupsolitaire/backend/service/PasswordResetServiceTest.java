package com.loupsolitaire.backend.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.loupsolitaire.backend.config.ProprietesDeTest;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PasswordResetTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

// Premiers tests de PasswordResetService (voir aussi le point 21 : le reste
// du parcours de reinitialisation reste a couvrir).
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

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

    @BeforeEach
    void setUp() {
        // Code valable 15 minutes, 5 essais maximum.
        service = new PasswordResetService(utilisateurRepository, tokenRepository, passwordEncoder, emailService,
                refreshTokenService, ProprietesDeTest.app());
    }

    @Test
    void supprimerTokensSupprimeLesDemandesDeL_utilisateur() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());

        service.supprimerTokens(utilisateur);

        verify(tokenRepository).deleteByUtilisateur(utilisateur);
    }
}