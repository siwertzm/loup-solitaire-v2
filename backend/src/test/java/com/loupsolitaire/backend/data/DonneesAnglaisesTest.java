package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.DisciplineJson;
import com.loupsolitaire.backend.dto.EnnemiJson;
import com.loupsolitaire.backend.dto.ObjetJson;
import com.loupsolitaire.backend.dto.RefJson;

import tools.jackson.databind.json.JsonMapper;

/**
 * DATA-19 : les fichiers data/en/*.json reprennent ceux de data/ avec le
 * texte officiel de la VO (Project Aon). Seuls les textes different : memes
 * entrees, dans le meme ordre, memes donnees de jeu.
 */
class DonneesAnglaisesTest {

    private static <T> List<T> lire(String chemin, Class<T> type) throws Exception {
        try (InputStream is = DonneesAnglaisesTest.class.getResourceAsStream(chemin)) {
            return JsonMapper.builder().build().readerForListOf(type).readValue(is);
        }
    }

    private static List<String> ids(List<RefJson> refs) {
        return refs == null ? List.of() : refs.stream().map(RefJson::getId).toList();
    }

    @Test
    void ennemisMemesQueLeFrancaisAuNomPres() throws Exception {
        List<EnnemiJson> fr = lire("/data/ennemi.json", EnnemiJson.class);
        List<EnnemiJson> en = lire("/data/en/ennemi.json", EnnemiJson.class);

        assertThat(en).hasSameSizeAs(fr);
        for (int i = 0; i < fr.size(); i++) {
            EnnemiJson f = fr.get(i);
            EnnemiJson e = en.get(i);
            assertThat(e.getId()).isEqualTo(f.getId());
            assertThat(e.getNom()).as(f.getId()).isNotBlank();
            assertThat(e.getHabilite()).as(f.getId()).isEqualTo(f.getHabilite());
            assertThat(e.getEndurance()).as(f.getId()).isEqualTo(f.getEndurance());
            assertThat(ids(e.getResistance())).as(f.getId()).isEqualTo(ids(f.getResistance()));
            assertThat(ids(e.getDisciplines())).as(f.getId()).isEqualTo(ids(f.getDisciplines()));
        }
    }

    @Test
    void disciplinesMemesQueLeFrancaisAvecNomEtDescription() throws Exception {
        List<DisciplineJson> fr = lire("/data/discipline.json", DisciplineJson.class);
        List<DisciplineJson> en = lire("/data/en/discipline.json", DisciplineJson.class);

        assertThat(en).extracting(DisciplineJson::getId)
                .containsExactlyElementsOf(fr.stream().map(DisciplineJson::getId).toList());
        assertThat(en).allSatisfy(d -> {
            assertThat(d.getNom()).as(d.getId()).isNotBlank();
            assertThat(d.getDescription()).as(d.getId()).isNotBlank();
        });
    }

    @Test
    void objetsMemesQueLeFrancaisAuNomPres() throws Exception {
        List<ObjetJson> fr = lire("/data/objet.json", ObjetJson.class);
        List<ObjetJson> en = lire("/data/en/objet.json", ObjetJson.class);

        assertThat(en).hasSameSizeAs(fr);
        for (int i = 0; i < fr.size(); i++) {
            ObjetJson f = fr.get(i);
            ObjetJson e = en.get(i);
            assertThat(e.getId()).isEqualTo(f.getId());
            assertThat(e.getCategorie()).as(f.getId()).isEqualTo(f.getCategorie());
            assertThat(e.getEffet()).as(f.getId()).usingRecursiveComparison().isEqualTo(f.getEffet());
            assertThat(e.getNom()).as(f.getId()).isNotBlank();
        }
    }
}
