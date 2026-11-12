package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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



}