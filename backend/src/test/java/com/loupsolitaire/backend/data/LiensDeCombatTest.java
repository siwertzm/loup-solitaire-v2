package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.LienJson;

/**
 * D-11 (DATA-05, DATA-11) : deux sortes de liens pour quitter un combat.
 * <ul>
 * <li>EVITEMENT : partir sans livrer le combat, avant le premier assaut
 * (112, 180). Une fois engage, il faut gagner ou utiliser FUITE.</li>
 * <li>FUITE N : bouton FUITE actif apres N assauts ; 0 = des la premiere
 * action, sans pouvoir eviter le combat (« at any stage of the fight »).</li>
 * </ul>
 * Un lien d'evitement ne doit jamais porter sur une phrase du livre qui
 * commence par « Si vous êtes vainqueur » : ce lien-la exige la victoire.
 */
class LiensDeCombatTest {

    @Test
    void aucunLienDEvitementNeReposeSurUneVictoire() {
        List<String> fautifs = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                boolean evitement = DonneesDuLivre.conditions(lien).stream()
                        .anyMatch(c -> "evitement".equalsIgnoreCase(c.getType()));
                if (evitement && phraseDuLien(chapitre, lien).startsWith("Si vous êtes vainqueur")) {
                    fautifs.add(chapitre.getId() + " → " + lien.getPage());
                }
            }
        }

        assertThat(fautifs).isEmpty();
    }

    // Relu dans la VO le 16/11/2026 : chaque lien de fuite ou d'evitement du
    // livre, avec son seuil. Un lien ajoute ou modifie doit etre ajoute ici.
    @Test
    void lesLiensDeFuiteEtDEvitementSuiventLeLivre() {
        Map<String, String> attendus = Map.of(
                "43 → 106", "fuite=3",      // « After three rounds »
                "112 → 248", "evitement=1", // « Or you may leave » : aussi apres victoire (DATA-18)
                "169 → 23", "fuite=1",      // « after the first round »
                "180 → 22", "evitement=null", // « Si vous souhaitez engager le combat »
                "191 → 234", "fuite=0",     // « during the fight »
                "220 → 234", "fuite=0",     // « at any time during the fight »
                "231 → 7", "fuite=2",       // « after 2 rounds »
                "339 → 7", "fuite=0");      // « at any stage of the fight »

        Map<String, String> trouves = new HashMap<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                for (CondJson c : DonneesDuLivre.conditions(lien)) {
                    if ("fuite".equalsIgnoreCase(c.getType()) || "evitement".equalsIgnoreCase(c.getType())) {
                        trouves.put(chapitre.getId() + " → " + lien.getPage(), c.getType() + "=" + c.getValeur());
                    }
                }
            }
        }

        assertThat(trouves).isEqualTo(attendus);
    }

    @Test
    void auChapitre112LeLien33ExigeLaVictoire() {
        ChapitreJson chapitre = DonneesDuLivre.chapitres().get(112);

        List<String> conditions = DonneesDuLivre.liens(chapitre).stream()
                .filter(lien -> "33".equals(lien.getPage()))
                .flatMap(lien -> DonneesDuLivre.conditions(lien).stream())
                .map(c -> c.getType() + "=" + c.getValeur())
                .toList();

        assertThat(conditions).containsExactly("victoire=1");
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