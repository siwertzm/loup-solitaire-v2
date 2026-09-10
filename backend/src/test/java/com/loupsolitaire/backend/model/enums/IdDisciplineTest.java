package com.loupsolitaire.backend.model.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdDisciplineTest {

    @Test
    void convertitChaqueValeurJsonConnue() {
        assertThat(IdDiscipline.fromJson("camouflage")).isEqualTo(IdDiscipline.CAMOUFLAGE);
        assertThat(IdDiscipline.fromJson("chasse")).isEqualTo(IdDiscipline.CHASSE);
        assertThat(IdDiscipline.fromJson("orientation")).isEqualTo(IdDiscipline.ORIENTATION);
        assertThat(IdDiscipline.fromJson("bouclier_psychique")).isEqualTo(IdDiscipline.BOUCLIER_PSYCHIQUE);
        assertThat(IdDiscipline.fromJson("puissance_psychique")).isEqualTo(IdDiscipline.PUISSANCE_PSYCHIQUE);
        assertThat(IdDiscipline.fromJson("communication_animale")).isEqualTo(IdDiscipline.COMMUNICATION_ANIMALE);
        assertThat(IdDiscipline.fromJson("maitrise_matiere")).isEqualTo(IdDiscipline.MAITRISE_MATIERE);
    }

    @Test
    void accepteLesDeuxSynonymesAvecEtSansAccentPourSixiemeSens() {
        assertThat(IdDiscipline.fromJson("sixieme_sens")).isEqualTo(IdDiscipline.SIXIEME_SENS);
        assertThat(IdDiscipline.fromJson("sixième_sens")).isEqualTo(IdDiscipline.SIXIEME_SENS);
    }

    @Test
    void accepteLesDeuxSynonymesAvecEtSansAccentPourGuerison() {
        assertThat(IdDiscipline.fromJson("guerison")).isEqualTo(IdDiscipline.GUERISON);
        assertThat(IdDiscipline.fromJson("guérison")).isEqualTo(IdDiscipline.GUERISON);
    }

    @Test
    void accepteLesDeuxSynonymesPourMaitriseDesArmes() {
        assertThat(IdDiscipline.fromJson("maitrise_armes")).isEqualTo(IdDiscipline.MAITRISE_ARMES);
        assertThat(IdDiscipline.fromJson("maitrise_des_armes")).isEqualTo(IdDiscipline.MAITRISE_ARMES);
    }

    @Test
    void ignoreLaCasseEtLesEspacesAutourDeLaValeur() {
        assertThat(IdDiscipline.fromJson("  CAMOUFLAGE  ")).isEqualTo(IdDiscipline.CAMOUFLAGE);
        assertThat(IdDiscipline.fromJson("Chasse")).isEqualTo(IdDiscipline.CHASSE);
    }

    @Test
    void leveUneExceptionSiLaValeurEstNull() {
        assertThatThrownBy(() -> IdDiscipline.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquant");
    }

    @Test
    void leveUneExceptionSiLaValeurEstInconnue() {
        assertThatThrownBy(() -> IdDiscipline.fromJson("telepathie"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telepathie");
    }
}
