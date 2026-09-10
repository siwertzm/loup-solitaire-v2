package com.loupsolitaire.backend.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TypeConditionTest {

    @Test
    void convertitChaqueValeurJsonConnue() {
        assertThat(TypeCondition.fromJson("discipline")).isEqualTo(TypeCondition.DISCIPLINE);
        assertThat(TypeCondition.fromJson("objet")).isEqualTo(TypeCondition.OBJET);
        assertThat(TypeCondition.fromJson("bourse")).isEqualTo(TypeCondition.BOURSE);
        assertThat(TypeCondition.fromJson("hasard")).isEqualTo(TypeCondition.HASARD);
        assertThat(TypeCondition.fromJson("fuite")).isEqualTo(TypeCondition.FUITE);
        assertThat(TypeCondition.fromJson("arme")).isEqualTo(TypeCondition.ARME);
        assertThat(TypeCondition.fromJson("endurance")).isEqualTo(TypeCondition.ENDURANCE);
        assertThat(TypeCondition.fromJson("endurance_perdue")).isEqualTo(TypeCondition.ENDURANCE_PERDUE);
        assertThat(TypeCondition.fromJson("assaut_max")).isEqualTo(TypeCondition.ASSAUT_MAX);
        assertThat(TypeCondition.fromJson("assaut_echec")).isEqualTo(TypeCondition.ASSAUT_ECHEC);
        assertThat(TypeCondition.fromJson("victoire")).isEqualTo(TypeCondition.VICTOIRE);
        assertThat(TypeCondition.fromJson("permanent")).isEqualTo(TypeCondition.PERMANENT);
    }

    @Test
    void ignoreLaCasseEtLesEspacesAutourDeLaValeur() {
        assertThat(TypeCondition.fromJson("  HASARD  ")).isEqualTo(TypeCondition.HASARD);
        assertThat(TypeCondition.fromJson("Victoire")).isEqualTo(TypeCondition.VICTOIRE);
    }

    @Test
    void leveUneExceptionSiLaValeurEstNull() {
        assertThatThrownBy(() -> TypeCondition.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquant");
    }

    @Test
    void leveUneExceptionSiLaValeurEstInconnue() {
        assertThatThrownBy(() -> TypeCondition.fromJson("mystere"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mystere");
    }
}
