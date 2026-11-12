package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.EffetJson;

/**
 * REGLE-11 / DATA-13 : au 260, le -4 du combat a mains nues est marque
 * « sans_arme ». Sans ce marqueur, il s'ajoutait a l'HABILETE temporaire et
 * se cumulait avec le -4 deja applique a un joueur sans arme (-8).
 */
class CombatAMainsNuesTest {

    @Test
    void leMalusDuParagraphe260EstMarqueSansArme() {
        ChapitreJson chapitre = DonneesDuLivre.chapitres().get(260);
        List<EffetJson> habilite = chapitre.getEffet().stream()
                .filter(effet -> "habilite".equalsIgnoreCase(effet.getType()))
                .toList();

        assertThat(habilite).singleElement().satisfies(effet -> {
            assertThat(effet.getValeur()).isEqualTo(-4);
            assertThat(effet.getCond()).singleElement()
                    .satisfies(cond -> assertThat(cond.getType()).isEqualToIgnoringCase("sans_arme"));
        });
    }

    @Test
    void leMarqueurSansArmeNeSertQuAu260() {
        List<Integer> chapitres = DonneesDuLivre.chapitres().values().stream()
                .filter(c -> c.getEffet() != null && c.getEffet().stream()
                        .anyMatch(e -> e.getCond() != null && e.getCond().stream()
                                .anyMatch(cond -> "sans_arme".equalsIgnoreCase(cond.getType()))))
                .map(ChapitreJson::getId)
                .toList();

        assertThat(chapitres).containsExactly(260);
    }
}
