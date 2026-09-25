package com.loupsolitaire.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokensTest {

    @Test
    void genererAleatoireProduitUnTokenUtilisableDansUneUrl() {
        String token = Tokens.genererAleatoire();

        // 32 octets en Base64 sans padding = 43 caracteres, alphabet URL safe.
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void genererAleatoireNeRenvoieJamaisDeuxFoisLaMemeValeur() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(Tokens.genererAleatoire());
        }

        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void genererCodeASixChiffresGardeLesZerosInitiaux() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(Tokens.genererCodeASixChiffres()).matches("\\d{6}");
        }
    }

    @Test
    void hacherRenvoieLeSha256EnHexadecimal() {
        // Valeur de reference connue du SHA-256 de "abc".
        assertThat(Tokens.hacher("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hacherEstDeterministe() {
        String token = Tokens.genererAleatoire();

        // Indispensable : c'est ce qui permet de retrouver le token en base.
        assertThat(Tokens.hacher(token)).isEqualTo(Tokens.hacher(token)).hasSize(64);
    }
}