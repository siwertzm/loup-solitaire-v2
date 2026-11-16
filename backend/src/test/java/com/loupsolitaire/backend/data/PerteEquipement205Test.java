package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.EffetJson;

/**
 * DATA-06 / DATA-07 : le chef des brigands ramasse l'equipement au 205 (VO :
 * « erase all Weapons and Backpack Items »). Le 181 n'enleve rien : on peut y
 * arriver par le 22 sans avoir rien donne.
 */
class PerteEquipement205Test {

    @Test
    void le205RetireArmesEtSac() {
        List<EffetJson> vols = vols(205);

        assertThat(vols).hasSize(1);
        assertThat(vols.get(0).getValeur()).isEqualTo(10);
        assertThat(vols.get(0).getCond()).isNullOrEmpty();
    }

    @Test
    void le181NeRetireRien() {
        assertThat(vols(181)).isEmpty();
        assertThat(DonneesDuLivre.chapitres().get(181).getText()).doesNotContain("perdu votre");
    }

    private static List<EffetJson> vols(int paragraphe) {
        ChapitreJson chapitre = DonneesDuLivre.chapitres().get(paragraphe);
        List<EffetJson> effets = chapitre.getEffet() == null ? List.of() : chapitre.getEffet();
        return effets.stream().filter(e -> "vol".equalsIgnoreCase(e.getType())).toList();
    }
}
