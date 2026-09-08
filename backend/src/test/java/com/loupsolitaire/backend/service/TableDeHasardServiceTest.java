package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableDeHasardServiceTest {

    @Mock
    private Random random;

    @Test
    void tirerChiffreRenvoieUnEntierEntre0Et9() {
        // On verifie que le service delegue bien a random.nextInt(10),
        // sans reimplementer la logique de Random lui-meme.
        when(random.nextInt(10)).thenReturn(7);

        TableDeHasardService service = new TableDeHasardService(random);

        assertThat(service.tirerChiffre()).isEqualTo(7);
    }

    @Test
    void tirerParmiRenvoieLElementCorrespondantAuTirage() {
        when(random.nextInt(9)).thenReturn(3);

        TableDeHasardService service = new TableDeHasardService(random);
        List<String> armes = List.of(
                "poignard", "glaive", "lance", "epee", "marteau", "sabre", "masse", "hache", "baton"
        );

        assertThat(service.tirerParmi(armes)).isEqualTo("epee");
    }

    @Test
    void tirerParmiRefuseUneListeVide() {
        TableDeHasardService service = new TableDeHasardService(random);

        assertThatThrownBy(() -> service.tirerParmi(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tirerParmiRefuseUneListeNulle() {
        TableDeHasardService service = new TableDeHasardService(random);

        assertThatThrownBy(() -> service.tirerParmi(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}