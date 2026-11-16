package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.EffetJson;
import com.loupsolitaire.backend.dto.ObjetChapJson;

/**
 * REGLE-12 : le Sac a Dos est perdu la ou la VO le dit (vol 11 = Sac + armes,
 * vol 9 = Sac seul), garde au 205 (vol 10 : seulement son contenu), et
 * ramassable aux 20 et 62.
 */
class SacADosTest {

    private static final Map<Integer, Integer> PERTES_DU_SAC = Map.of(
            162, 11, 174, 11, 258, 11, 294, 11, 188, 9, 205, 10);

    @Test
    void leSacEstPerduLaOuLeLivreLeDit() {
        PERTES_DU_SAC.forEach((paragraphe, valeur) -> {
            List<EffetJson> vols = vols(paragraphe);
            assertThat(vols).as("vol au %d", paragraphe).hasSize(1);
            assertThat(vols.get(0).getValeur()).as("vol au %d", paragraphe).isEqualTo(valeur);
        });
    }

    @Test
    void auCent88LeSacNEstPerduQueSurUnTirageDeZeroASix() {
        EffetJson vol = vols(188).get(0);

        assertThat(vol.getCond()).singleElement().satisfies(c -> {
            assertThat(c.getType()).isEqualToIgnoringCase("hasard");
            assertThat(c.getValeur()).isEqualTo("[0, 6]");
        });
    }

    @Test
    void unSacEstRamassableAux20Et62() {
        for (int paragraphe : List.of(20, 62)) {
            List<ObjetChapJson> objets = DonneesDuLivre.chapitres().get(paragraphe).getObjet();
            assertThat(objets).as("objets du %d", paragraphe)
                    .anySatisfy(o -> {
                        assertThat(o.getId()).isEqualTo("sac_a_dos");
                        assertThat(o.getValeur()).isEqualTo(1);
                        assertThat(o.isOptionnel()).isTrue();
                    });
        }
    }

    private static List<EffetJson> vols(int paragraphe) {
        ChapitreJson chapitre = DonneesDuLivre.chapitres().get(paragraphe);
        List<EffetJson> effets = chapitre.getEffet() == null ? List.of() : chapitre.getEffet();
        return effets.stream().filter(e -> "vol".equalsIgnoreCase(e.getType())).toList();
    }
}
