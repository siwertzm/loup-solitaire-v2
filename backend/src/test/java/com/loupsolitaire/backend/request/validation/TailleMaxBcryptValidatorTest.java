package com.loupsolitaire.backend.request.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TailleMaxBcryptValidatorTest {

    private final TailleMaxBcryptValidator validator = new TailleMaxBcryptValidator();

    @Test
    void accepteJusquA72Octets() {
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
        assertThat(validator.isValid("motdepasse123", null)).isTrue();
    }

    @Test
    void refuseAuDelaDe72Octets() {
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    void compteLesOctetsEtPasLesCaracteres() {
        // 40 caracteres, mais 80 octets en UTF-8 ("é" = 2 octets).
        assertThat(validator.isValid("é".repeat(40), null)).isFalse();
        // 36 "é" = 72 octets : encore accepte.
        assertThat(validator.isValid("é".repeat(36), null)).isTrue();
    }

    @Test
    void laisseNullAuxAutresContraintes() {
        // null est refuse par @NotBlank, pas par cette contrainte.
        assertThat(validator.isValid(null, null)).isTrue();
    }
}