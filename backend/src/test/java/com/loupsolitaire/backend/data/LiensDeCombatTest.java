package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.LienJson;

/**
 * DATA-05 : un lien "fuite = 0" permet d'eviter le combat (bouton a cote de
 * COMBAT sur la page du chapitre). Il ne doit donc jamais porter sur une
 * phrase du livre qui commence par « Si vous êtes vainqueur » : ce lien-la
 * exige la victoire.
 *
 * Le cas du 112 → 248 (« Mais vous pouvez également quitter les lieux ») a
 * ete tranche : il reste en fuite = 0 (DATA-05 « Déjà OK »).
 */
class LiensDeCombatTest {

    @Test
    void aucunLienDeFuiteAZeroNeReposeSurUneVictoire() {
        List<String> fautifs = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                boolean fuiteAZero = DonneesDuLivre.conditions(lien).stream()
                        .anyMatch(c -> "fuite".equalsIgnoreCase(c.getType()) && "0".equals(c.getValeur()));
                if (fuiteAZero && phraseDuLien(chapitre, lien).startsWith("Si vous êtes vainqueur")) {
                    fautifs.add(chapitre.getId() + " → " + lien.getPage());
                }
            }
        }

        assertThat(fautifs).isEmpty();
    }

    @Test
    void chaqueLienDeVictoireEstDansUnChapitreDeCombat() {
        List<String> fautifs = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                for (CondJson condition : DonneesDuLivre.conditions(lien)) {
                    if ("victoire".equalsIgnoreCase(condition.getType()) && !chapitre.isCombat()) {
                        fautifs.add(chapitre.getId() + " → " + lien.getPage());
                    }
                }
            }
        }

        assertThat(fautifs).isEmpty();
    }

    /** La ligne du texte qui renvoie vers la cible du lien. */
    private static String phraseDuLien(ChapitreJson chapitre, LienJson lien) {
        String cible = "<strong>" + lien.getPage() + "</strong>";
        return chapitre.getText().lines()
                .filter(ligne -> ligne.contains(cible))
                .findFirst()
                .orElse("")
                .trim();
    }
}