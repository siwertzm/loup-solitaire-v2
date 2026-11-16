package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ObjetChapJson;
import com.loupsolitaire.backend.dto.ObjetJson;

import tools.jackson.databind.json.JsonMapper;

/**
 * DATA-14 (decision D-04, VO) : au 137, les 20 Pierres comptent pour un seul
 * objet du Sac a Dos, pas pour 20 pieces de la bourse.
 */
class PierresDu137Test {

    @Test
    void le137DonneUnSeulObjetDeSac() throws IOException {
        List<ObjetChapJson> objets = DonneesDuLivre.chapitres().get(137).getObjet();

        assertThat(objets).singleElement().satisfies(o -> {
            assertThat(o.getId()).isEqualTo("pierres_tombeau");
            assertThat(o.getValeur()).isEqualTo(1);
        });
        assertThat(catalogue()).anySatisfy(o -> {
            assertThat(o.getId()).isEqualTo("pierres_tombeau");
            assertThat(o.getCategorie()).isEqualTo("objet");
        });
    }

    @Test
    void aucunChapitreNeDonnePlusDePierresDansLaBourse() {
        assertThat(DonneesDuLivre.chapitres().values())
                .flatExtracting(c -> c.getObjet() == null ? List.of() : c.getObjet())
                .extracting("id")
                .doesNotContain("pierre_precieuse");
    }

    private static List<ObjetJson> catalogue() throws IOException {
        try (InputStream is = PierresDu137Test.class.getResourceAsStream("/data/objet.json")) {
            return JsonMapper.builder().build().readerForListOf(ObjetJson.class).readValue(is);
        }
    }
}
