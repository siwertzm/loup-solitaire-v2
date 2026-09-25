package com.loupsolitaire.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.loupsolitaire.backend.model.enums.TypeCondition;

class CondTest {

    private Cond cond(TypeCondition type, String valeur) {
        Cond cond = new Cond();
        cond.setType(type);
        cond.setValeur(valeur);
        return cond;
    }

    // =========================================================
    // valeurEntiere
    // =========================================================

    @Test
    void valeurEntiereLitUnNombre() {
        assertThat(cond(TypeCondition.ENDURANCE, "10").valeurEntiere()).isEqualTo(10);
        assertThat(cond(TypeCondition.ENDURANCE, " 4 ").valeurEntiere()).isEqualTo(4);
        // "-1" : "si vous ne possedez PAS" (voir EffetChapitreService).
        assertThat(cond(TypeCondition.DISCIPLINE, "-1").valeurEntiere()).isEqualTo(-1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "dix", "1.5", "[0, 4]"})
    void valeurEntiereRefuseUneValeurNonNumerique(String valeur) {
        assertThatThrownBy(() -> cond(TypeCondition.ENDURANCE, valeur).valeurEntiere())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENDURANCE");
    }

    // =========================================================
    // plageHasard
    // =========================================================

    @Test
    void plageHasardLitLesDeuxBornes() {
        Cond.PlageHasard plage = cond(TypeCondition.HASARD, "[3, 6]").plageHasard();

        assertThat(plage.min()).isEqualTo(3);
        assertThat(plage.max()).isEqualTo(6);
        assertThat(plage.contient(3)).isTrue();
        assertThat(plage.contient(6)).isTrue();
        assertThat(plage.contient(2)).isFalse();
        assertThat(plage.contient(7)).isFalse();
    }

    @Test
    void plageHasardTolereLesEspaces() {
        assertThat(cond(TypeCondition.HASARD, "[9,9]").plageHasard()).isEqualTo(new Cond.PlageHasard(9, 9));
        assertThat(cond(TypeCondition.HASARD, " [ 0 , 4 ] ").plageHasard()).isEqualTo(new Cond.PlageHasard(0, 4));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "4", "0-4", "[0 4]", "[0, 10]", "[-1, 4]", "[5, 2]"})
    void plageHasardRefuseUnFormatInvalide(String valeur) {
        assertThatThrownBy(() -> cond(TypeCondition.HASARD, valeur).plageHasard())
                .isInstanceOf(IllegalStateException.class);
    }
}