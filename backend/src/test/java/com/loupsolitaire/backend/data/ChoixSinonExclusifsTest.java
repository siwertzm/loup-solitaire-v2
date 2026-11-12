package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.LienJson;

/**
 * REGLE-14 : « Si vous possedez X, rendez-vous au A. Sinon, au B. » Le lien
 * vers B porte la meme condition que le lien vers A, avec la valeur -1
 * (« ne possede pas ») : un seul des deux est disponible, jamais les deux.
 */
class ChoixSinonExclusifsTest {

    // Paragraphe -> lien "Sinon" (texte : « Sinon », « Dans le cas
    // contraire », « Si vous n'avez pas cette Cle »).
    private static final Map<Integer, String> CHOIX_SINON = Map.of(
            9, "292", 52, "250", 105, "335", 128, "336", 162, "127",
            173, "259", 242, "9", 272, "305", 303, "72");

    @Test
    void chaqueLienSinonEstLeContraireExactDeLAutreLien() {
        List<String> ecarts = new ArrayList<>();
        CHOIX_SINON.forEach((paragraphe, pageSinon) -> {
            ChapitreJson chapitre = DonneesDuLivre.chapitres().get(paragraphe);
            List<LienJson> liens = DonneesDuLivre.liens(chapitre);
            LienJson sinon = liens.stream().filter(l -> pageSinon.equals(l.getPage())).findFirst().orElse(null);
            LienJson choix = liens.stream().filter(l -> !pageSinon.equals(l.getPage())).findFirst().orElse(null);
            if (liens.size() != 2 || sinon == null || choix == null) {
                ecarts.add(paragraphe + " : deux liens attendus");
                return;
            }
            List<CondJson> condSinon = DonneesDuLivre.conditions(sinon);
            List<CondJson> condChoix = DonneesDuLivre.conditions(choix);
            if (condSinon.size() != 1 || condChoix.size() != 1) {
                ecarts.add(paragraphe + " : une condition attendue sur chaque lien");
                return;
            }
            CondJson a = condChoix.get(0);
            CondJson b = condSinon.get(0);
            boolean contraire = a.getType().equalsIgnoreCase(b.getType())
                    && a.getTargetId().equals(b.getTargetId())
                    && "1".equals(a.getValeur()) && "-1".equals(b.getValeur());
            if (!contraire) {
                ecarts.add(paragraphe + " : le lien vers " + pageSinon + " n'est pas le contraire de l'autre");
            }
        });
        assertThat(ecarts).isEmpty();
    }
}
