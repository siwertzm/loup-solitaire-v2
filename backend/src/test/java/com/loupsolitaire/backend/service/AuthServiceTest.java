package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.loupsolitaire.backend.config.JwtUtil;
import com.loupsolitaire.backend.exception.TropDeRequetesException;
import com.loupsolitaire.backend.exception.CompteNonVerifieException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.AuthResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LimiteurDeDebit limiteurDeDebit;

    @InjectMocks
    private AuthService authService;

    private Utilisateur utilisateur(boolean emailVerifie) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setUsername("marius");
        utilisateur.setEmailVerifie(emailVerifie);
        return utilisateur;
    }

    private void authentificationReussie() {
        UserDetails userDetails = User.builder().username("marius").password("hash").authorities("ROLE_USER").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
    }

    @Test
    void connecterOuvreUneSessionPourUnCompteVerifie() {
        Utilisateur utilisateur = utilisateur(true);
        authentificationReussie();
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        // Le token porte l'identifiant (UUID) de l'utilisateur, pas son nom.
        when(jwtUtil.generateToken(utilisateur.getId())).thenReturn("access-token");
        when(refreshTokenService.creerToken(utilisateur)).thenReturn("refresh-token");

        AuthResponse reponse = authService.connecter("marius", "motdepasse123");

        assertThat(reponse.accessToken()).isEqualTo("access-token");
        assertThat(reponse.refreshToken()).isEqualTo("refresh-token");
        assertThat(reponse.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void connecterBloqueUnCompteNonVerifieSansCreerDeSession() {
        authentificationReussie();
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur(false)));

        assertThatThrownBy(() -> authService.connecter("marius", "motdepasse123"))
                .isInstanceOf(CompteNonVerifieException.class);

        verify(jwtUtil, never()).generateToken(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void connecterLaissePasserLEchecD_authentification() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.connecter("marius", "mauvais"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(utilisateurRepository, jwtUtil, refreshTokenService);
    }

    @Test
    void connecterCompteChaqueEchecPourL_identifiant() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.connecter("marius", "mauvais"))
                .isInstanceOf(BadCredentialsException.class);

        verify(limiteurDeDebit).enregistrerEchec(LimiteurDeDebit.LOGIN_COMPTE, "marius");
        verify(limiteurDeDebit, never()).reinitialiser(any(), any());
    }

    @Test
    void connecterRemetLeCompteurAZeroApresUnSucces() {
        Utilisateur utilisateur = utilisateur(true);
        authentificationReussie();
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        authService.connecter("marius", "motdepasse123");

        verify(limiteurDeDebit).reinitialiser(LimiteurDeDebit.LOGIN_COMPTE, "marius");
    }

    @Test
    void connecterRefuseEn429SansVerifierLeMotDePasseQuandLaLimiteEstAtteinte() {
        doThrow(new TropDeRequetesException(600))
                .when(limiteurDeDebit).verifier(LimiteurDeDebit.LOGIN_COMPTE, "marius");

        assertThatThrownBy(() -> authService.connecter("marius", "motdepasse123"))
                .isInstanceOf(TropDeRequetesException.class);

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void rafraichirPivoteLeRefreshTokenEtGenereUnAccessToken() {
        UUID utilisateurId = UUID.randomUUID();
        when(refreshTokenService.validerEtPivoter("ancien"))
                .thenReturn(new RefreshTokenService.RotationResult(utilisateurId, "nouveau-refresh"));
        when(jwtUtil.generateToken(utilisateurId)).thenReturn("nouveau-access");

        AuthResponse reponse = authService.rafraichir("ancien");

        assertThat(reponse.accessToken()).isEqualTo("nouveau-access");
        assertThat(reponse.refreshToken()).isEqualTo("nouveau-refresh");
    }

    @Test
    void deconnecterRevoqueLeRefreshToken() {
        authService.deconnecter("un-refresh-token");

        verify(refreshTokenService).revoquer("un-refresh-token");
    }
}