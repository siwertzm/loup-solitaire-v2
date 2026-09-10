package com.loupsolitaire.backend.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CategorieObjetTest {

    @Test
    void convertitChaqueValeurJsonConnue() {
        assertThat(CategorieObjet.fromJson("arme")).isEqualTo(CategorieObjet.ARME);
        assertThat(CategorieObjet.fromJson("objet")).isEqualTo(CategorieObjet.OBJET);
        assertThat(CategorieObjet.fromJson("repas")).isEqualTo(CategorieObjet.REPAS);
        assertThat(CategorieObjet.fromJson("bourse")).isEqualTo(CategorieObjet.BOURSE);
    }

    @Test
    void accepteLesDeuxSynonymesAvecEtSansAccentPourObjetsSpeciaux() {
        assertThat(CategorieObjet.fromJson("objets spéciaux")).isEqualTo(CategorieObjet.OBJETS_SPECIAUX);
        assertThat(CategorieObjet.fromJson("objets speciaux")).isEqualTo(CategorieObjet.OBJETS_SPECIAUX);
    }

    @Test
    void ignoreLaCasseEtLesEspacesAutourDeLaValeur() {
        assertThat(CategorieObjet.fromJson("  ARME  ")).isEqualTo(CategorieObjet.ARME);
        assertThat(CategorieObjet.fromJson("Repas")).isEqualTo(CategorieObjet.REPAS);
    }

    @Test
    void leveUneExceptionSiLaValeurEstNull() {
        assertThatThrownBy(() -> CategorieObjet.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquante");
    }

    @Test
    void leveUneExceptionSiLaValeurEstInconnue() {
        assertThatThrownBy(() -> CategorieObjet.fromJson("artefact"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artefact");
    }
}
