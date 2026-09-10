package com.loupsolitaire.backend.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TypeEffetTest {

    @Test
    void convertitChaqueValeurJsonConnue() {
        assertThat(TypeEffet.fromJson("endurance")).isEqualTo(TypeEffet.ENDURANCE);
        assertThat(TypeEffet.fromJson("habilite")).isEqualTo(TypeEffet.HABILETE);
        assertThat(TypeEffet.fromJson("repas")).isEqualTo(TypeEffet.REPAS);
        assertThat(TypeEffet.fromJson("vol")).isEqualTo(TypeEffet.VOL);
        assertThat(TypeEffet.fromJson("echange")).isEqualTo(TypeEffet.ECHANGE);
    }

    @Test
    void ignoreLaCasseEtLesEspacesAutourDeLaValeur() {
        assertThat(TypeEffet.fromJson("  ENDURANCE  ")).isEqualTo(TypeEffet.ENDURANCE);
        assertThat(TypeEffet.fromJson("Vol")).isEqualTo(TypeEffet.VOL);
    }

    @Test
    void leveUneExceptionSiLaValeurEstNull() {
        assertThatThrownBy(() -> TypeEffet.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquant");
    }

    @Test
    void leveUneExceptionSiLaValeurEstInconnue() {
        assertThatThrownBy(() -> TypeEffet.fromJson("malediction"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("malediction");
    }
}
