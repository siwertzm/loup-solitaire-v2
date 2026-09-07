package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.RefreshToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 30L);

        utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setUsername("marius");
        utilisateur.setPassword("hash");
    }

    @Test
    void creerTokenPersisteUnHashEtRenvoieLaValeurBrute() {
        String rawToken = service.creerToken(utilisateur);

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken sauvegarde = captor.getValue();
        assertThat(sauvegarde.getUtilisateur()).isEqualTo(utilisateur);
        assertThat(sauvegarde.getTokenHash()).isNotBlank();
        // Le hash ne doit jamais etre egal a la valeur brute transmise au client.
        assertThat(sauvegarde.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(sauvegarde.isRevoked()).isFalse();
        assertThat(sauvegarde.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void validerEtPivoterFaitTournerLeTokenEtRenvoieLesInfosUtilisateur() {
        RefreshToken existant = new RefreshToken();
        existant.setUtilisateur(utilisateur);
        existant.setRevoked(false);
        existant.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existant));

        RefreshTokenService.RotationResult resultat = service.validerEtPivoter("un-token-quelconque");

        assertThat(resultat.username()).isEqualTo("marius");
        assertThat(resultat.passwordHash()).isEqualTo("hash");
        assertThat(resultat.nouveauRefreshToken()).isNotBlank();
        assertThat(existant.isRevoked()).isTrue();
        // Une fois pivote, l'ancien est sauvegarde comme revoque et un nouveau est cree.
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void reutiliserUnTokenDejaRevoqueDeclencheLaRevocationDeToutesLesSessions() {
        RefreshToken dejaUtilise = new RefreshToken();
        dejaUtilise.setUtilisateur(utilisateur);
        dejaUtilise.setRevoked(true);
        dejaUtilise.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(dejaUtilise));
        when(refreshTokenRepository.findAllByUtilisateurAndRevokedFalse(utilisateur))
                .thenReturn(List.of(dejaUtilise));

        assertThatThrownBy(() -> service.validerEtPivoter("token-vole"))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("Reutilisation");

        verify(refreshTokenRepository).findAllByUtilisateurAndRevokedFalse(utilisateur);
    }

    @Test
    void unTokenExpireEstRejeteSansRevoquerLesAutresSessions() {
        RefreshToken expire = new RefreshToken();
        expire.setUtilisateur(utilisateur);
        expire.setRevoked(false);
        expire.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expire));

        assertThatThrownBy(() -> service.validerEtPivoter("token-perime"))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("expire");

        verify(refreshTokenRepository, never()).findAllByUtilisateurAndRevokedFalse(any());
    }

    @Test
    void unTokenInconnuEstRejete() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validerEtPivoter("token-inexistant"))
                .isInstanceOf(TokenInvalideException.class);
    }

    @Test
    void revoquerMarqueLeTokenExistantCommeRevoque() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.revoquer("un-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revoquerNeFaitRienSiLeTokenEstIntrouvable() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revoquer("token-inexistant");

        verify(refreshTokenRepository, never()).save(any());
    }
}