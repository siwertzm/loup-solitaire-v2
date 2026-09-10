package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TableCombatServiceTest {

    private final TableCombatService tableCombatService = new TableCombatService();

    // =========================================================
    // diffVersColonne : traduit un rapport de combat en index de colonne
    // (0 a 12), clampe aux extremes.
    // =========================================================

    @Test
    void clampeLesRapportsTresNegatifsSurLaColonne0() {
        assertThat(tableCombatService.diffVersColonne(-11)).isZero();
        assertThat(tableCombatService.diffVersColonne(-12)).isZero();
        assertThat(tableCombatService.diffVersColonne(-999)).isZero();
    }

    @Test
    void clampeLesRapportsTresPositifsSurLaColonne12() {
        assertThat(tableCombatService.diffVersColonne(11)).isEqualTo(12);
        assertThat(tableCombatService.diffVersColonne(12)).isEqualTo(12);
        assertThat(tableCombatService.diffVersColonne(999)).isEqualTo(12);
    }

    @Test
    void traduitChaqueBorneDeColonneCorrectement() {
        assertThat(tableCombatService.diffVersColonne(-10)).isEqualTo(1);
        assertThat(tableCombatService.diffVersColonne(-9)).isEqualTo(1);
        assertThat(tableCombatService.diffVersColonne(-8)).isEqualTo(2);
        assertThat(tableCombatService.diffVersColonne(-7)).isEqualTo(2);
        assertThat(tableCombatService.diffVersColonne(-6)).isEqualTo(3);
        assertThat(tableCombatService.diffVersColonne(-5)).isEqualTo(3);
        assertThat(tableCombatService.diffVersColonne(-4)).isEqualTo(4);
        assertThat(tableCombatService.diffVersColonne(-3)).isEqualTo(4);
        assertThat(tableCombatService.diffVersColonne(-2)).isEqualTo(5);
        assertThat(tableCombatService.diffVersColonne(-1)).isEqualTo(5);
        assertThat(tableCombatService.diffVersColonne(0)).isEqualTo(6);
        assertThat(tableCombatService.diffVersColonne(1)).isEqualTo(7);
        assertThat(tableCombatService.diffVersColonne(2)).isEqualTo(7);
        assertThat(tableCombatService.diffVersColonne(3)).isEqualTo(8);
        assertThat(tableCombatService.diffVersColonne(4)).isEqualTo(8);
        assertThat(tableCombatService.diffVersColonne(5)).isEqualTo(9);
        assertThat(tableCombatService.diffVersColonne(6)).isEqualTo(9);
        assertThat(tableCombatService.diffVersColonne(7)).isEqualTo(10);
        assertThat(tableCombatService.diffVersColonne(8)).isEqualTo(10);
        assertThat(tableCombatService.diffVersColonne(9)).isEqualTo(11);
        assertThat(tableCombatService.diffVersColonne(10)).isEqualTo(11);
    }

    // =========================================================
    // degatsInfliges (TABLE_DEGATS_INFLIGES / "TABLE_LS")
    // =========================================================

    @Test
    void degatsInfligesColonne0RapportTresDefavorable() {
        // diff=-11 -> colonne 0 : { -6, 0, 0, 0, 0, -1, -2, -3, -4, -5 }
        assertThat(tableCombatService.degatsInfliges(-11, 0)).isEqualTo(-6);
        assertThat(tableCombatService.degatsInfliges(-11, 3)).isEqualTo(0);
        assertThat(tableCombatService.degatsInfliges(-11, 9)).isEqualTo(-5);
    }

    @Test
    void degatsInfligesColonne6RapportEgal() {
        // diff=0 -> colonne 6 : { -12, -3, -4, -5, -6, -7, -8, -9, -10, -11 }
        assertThat(tableCombatService.degatsInfliges(0, 0)).isEqualTo(-12);
        assertThat(tableCombatService.degatsInfliges(0, 5)).isEqualTo(-7);
        assertThat(tableCombatService.degatsInfliges(0, 9)).isEqualTo(-11);
    }

    @Test
    void degatsInfligesColonne12RapportTresFavorablePeutEtreUnCoupNet() {
        // diff=11 -> colonne 12 : { -999, -9, -10, -11, -12, -14, -16, -18, -999, -999 }
        assertThat(tableCombatService.degatsInfliges(11, 0)).isEqualTo(-999);
        assertThat(tableCombatService.degatsInfliges(11, 1)).isEqualTo(-9);
        assertThat(tableCombatService.degatsInfliges(11, 7)).isEqualTo(-18);
        assertThat(tableCombatService.degatsInfliges(11, 8)).isEqualTo(-999);
        assertThat(tableCombatService.degatsInfliges(11, 9)).isEqualTo(-999);
    }

    // =========================================================
    // degatsSubis (TABLE_DEGATS_SUBIS / "TABLE_E")
    // =========================================================

    @Test
    void degatsSubisColonne0PeutEtreUnCoupNetDesDeuxCotes() {
        // diff=-11 -> colonne 0 : { -999, 0, -3, -4, -5, -6, -7, -8, -9, -999 }
        assertThat(tableCombatService.degatsSubis(-11, 0)).isEqualTo(-999);
        assertThat(tableCombatService.degatsSubis(-11, 1)).isEqualTo(0);
        assertThat(tableCombatService.degatsSubis(-11, 9)).isEqualTo(-999);
    }

    @Test
    void degatsSubisColonne6RapportEgal() {
        // diff=0 -> colonne 6 : { -5, 0, 0, -1, -1, -2, -2, -3, -4, -4 }
        assertThat(tableCombatService.degatsSubis(0, 0)).isEqualTo(-5);
        assertThat(tableCombatService.degatsSubis(0, 2)).isEqualTo(0);
        assertThat(tableCombatService.degatsSubis(0, 9)).isEqualTo(-4);
    }

    @Test
    void degatsSubisColonne12RapportTresFavorableAuJoueur() {
        // diff=11 -> colonne 12 : { -3, 0, 0, 0, 0, -1, -1, -2, -2, -2 }
        assertThat(tableCombatService.degatsSubis(11, 0)).isEqualTo(-3);
        assertThat(tableCombatService.degatsSubis(11, 3)).isEqualTo(0);
        assertThat(tableCombatService.degatsSubis(11, 9)).isEqualTo(-2);
    }

    // =========================================================
    // reductionDefense / bonusHabilite : tirage 0-9 pendant une DEFENSE
    // =========================================================

    @Test
    void reductionDefenseSuitLaTablePourChaqueTirage() {
        int[] attendu = { 100, 25, 30, 40, 50, 60, 70, 80, 85, 90 };
        for (int tirage = 0; tirage < attendu.length; tirage++) {
            assertThat(tableCombatService.reductionDefense(tirage)).isEqualTo(attendu[tirage]);
        }
    }

    @Test
    void bonusHabiliteSuitLaTablePourChaqueTirage() {
        int[] attendu = { 5, 1, 1, 2, 2, 3, 3, 3, 4, 4 };
        for (int tirage = 0; tirage < attendu.length; tirage++) {
            assertThat(tableCombatService.bonusHabilite(tirage)).isEqualTo(attendu[tirage]);
        }
    }

    @Test
    void unTirageDeZeroDonneLeBlocageTotalEtLeBonusHabiliteLePlusEleve() {
        // Constat factuel sur les donnees reelles de la table (voir la
        // divergence signalee avec le commentaire du service ci-dessus) :
        // tirage=0 -> 100% de reduction ET le bonus le plus fort (5), pas 0.
        assertThat(tableCombatService.reductionDefense(0)).isEqualTo(100);
        assertThat(tableCombatService.bonusHabilite(0)).isEqualTo(5);
    }
}