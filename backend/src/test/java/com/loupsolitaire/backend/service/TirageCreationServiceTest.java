package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.loupsolitaire.backend.model.TirageCreation;
import com.loupsolitaire.backend.repository.TirageCreationRepository;

@ExtendWith(MockitoExtension.class)
class TirageCreationServiceTest {

    @Mock
    private TirageCreationRepository tirageCreationRepository;
    @Mock
    private TableDeHasardService tableDeHasardService;

    @InjectMocks
    private TirageCreationService tirageCreationService;

    private final UUID utilisateurId = UUID.randomUUID();

    private TirageCreation tirageExistant(int hasardHabilite, int hasardEndurance) {
        TirageCreation tirage = new TirageCreation();
        tirage.setUtilisateurId(utilisateurId);
        tirage.setHasardHabilite(hasardHabilite);
        tirage.setHasardEndurance(hasardEndurance);
        return tirage;
    }

    @Test
    void premierTirageTireDeuxChiffresCoteServeurEtLesEnregistre() {
        when(tirageCreationRepository.findById(utilisateurId)).thenReturn(Optional.empty());
        when(tableDeHasardService.tirerChiffre()).thenReturn(4, 7);
        when(tirageCreationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TirageCreation tirage = tirageCreationService.tirerOuRelire(utilisateurId);

        assertThat(tirage.getUtilisateurId()).isEqualTo(utilisateurId);
        assertThat(tirage.getHasardHabilite()).isEqualTo(4);
        assertThat(tirage.getHasardEndurance()).isEqualTo(7);
        assertThat(tirage.getCreeLe()).isNotNull();
        verify(tirageCreationRepository).save(tirage);
    }

    @Test
    void unNouvelAppelRenvoieLeMemeTirageSansRelancer() {
        TirageCreation existant = tirageExistant(2, 9);
        when(tirageCreationRepository.findById(utilisateurId)).thenReturn(Optional.of(existant));

        assertThat(tirageCreationService.tirerOuRelire(utilisateurId)).isSameAs(existant);

        verifyNoInteractions(tableDeHasardService);
        verify(tirageCreationRepository, never()).save(any());
    }

    @Test
    void consommerRenvoieLeTirageEtLeSupprime() {
        TirageCreation existant = tirageExistant(5, 3);
        when(tirageCreationRepository.findById(utilisateurId)).thenReturn(Optional.of(existant));

        assertThat(tirageCreationService.consommer(utilisateurId)).isSameAs(existant);

        verify(tirageCreationRepository).delete(existant);
    }

    @Test
    void consommerSansTirageEnAttenteRenvoie400() {
        when(tirageCreationRepository.findById(utilisateurId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tirageCreationService.consommer(utilisateurId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tirage");

        verify(tirageCreationRepository, never()).delete(any());
    }
}