package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * QUAL-21 : les tables affichees dans la page des regles (frontend) doivent
 * etre celles du serveur. Changer une valeur d'un seul cote fait echouer ce
 * test : reporter le changement dans l'autre fichier.
 */
class TablesCombatPageReglesTest {

    // Tests lances depuis backend/ ; le monorepo est complet en CI.
    private static final Path FICHIER_FRONT =
            Path.of("../frontend/src/app/features/regle/combat/table-combat.data.ts");
    private static final int FATAL_SERVEUR = -999;
    // Un ecart d'HABILETE par colonne (voir TableCombatService.diffVersColonne).
    private static final int[] ECART_PAR_COLONNE = { -11, -10, -8, -6, -4, -2, 0, 1, 3, 5, 7, 9, 11 };

    private static String source;
    private final TableCombatService tables = new TableCombatService();

    @BeforeAll
    static void lireLeFichierDuFront() throws IOException {
        source = Files.readString(FICHIER_FRONT);
    }

    // Contenu de "export const NOM ... = [ ... ];", une liste par ligne de tableau.
    private static List<int[]> tableau(String nom) {
        Matcher m = Pattern.compile("export const " + nom + "\\b[^=]*=\\s*\\[(.*?)\\];", Pattern.DOTALL)
                .matcher(source);
        assertThat(m.find()).as("tableau %s introuvable dans %s", nom, FICHIER_FRONT).isTrue();
        String contenu = m.group(1);
        if (!contenu.contains("[")) {
            return List.of(valeurs(contenu));
        }
        Matcher ligne = Pattern.compile("\\[([^\\[\\]]*)\\]").matcher(contenu);
        return ligne.results().map(r -> valeurs(r.group(1))).toList();
    }

    // FATAL (front) devient -999 (serveur) ; les degats du front sont positifs.
    private static int[] valeurs(String liste) {
        return Arrays.stream(liste.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .mapToInt(v -> v.equals("FATAL") ? FATAL_SERVEUR : -Integer.parseInt(v))
                .toArray();
    }

    private void verifierDegats(String nom, DegatsServeur serveur) {
        List<int[]> front = tableau(nom);
        assertThat(front).as("%s : nombre de colonnes", nom).hasSize(ECART_PAR_COLONNE.length);
        for (int col = 0; col < front.size(); col++) {
            assertThat(front.get(col)).as("%s, colonne %d : nombre de chiffres", nom, col).hasSize(10);
            for (int chiffre = 0; chiffre < 10; chiffre++) {
                assertThat(front.get(col)[chiffre])
                        .as("%s, colonne %d, chiffre %d (front en negatif)", nom, col, chiffre)
                        .isEqualTo(serveur.degats(ECART_PAR_COLONNE[col], chiffre));
            }
        }
    }

    private interface DegatsServeur {
        int degats(int ecart, int chiffre);
    }

    @Test
    void degatsInfligesIdentiques() {
        verifierDegats("DEGATS_INFLIGES", tables::degatsInfliges);
    }

    @Test
    void degatsSubisIdentiques() {
        verifierDegats("DEGATS_SUBIS", tables::degatsSubis);
    }

    @Test
    void defenseIdentique() {
        int[] reduction = tableau("REDUCTION_DEFENSE").get(0);
        int[] bonus = tableau("BONUS_HABILITE_DEFENSE").get(0);
        assertThat(reduction).hasSize(10);
        assertThat(bonus).hasSize(10);
        for (int chiffre = 0; chiffre < 10; chiffre++) {
            // valeurs() inverse le signe : ces deux tables sont positives des deux cotes.
            assertThat(-reduction[chiffre]).as("REDUCTION_DEFENSE, chiffre %d", chiffre)
                    .isEqualTo(tables.reductionDefense(chiffre));
            assertThat(-bonus[chiffre]).as("BONUS_HABILITE_DEFENSE, chiffre %d", chiffre)
                    .isEqualTo(tables.bonusHabilite(chiffre));
        }
    }
}
