package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.DisciplineJson;
import com.loupsolitaire.backend.dto.EnnemiJson;
import com.loupsolitaire.backend.dto.LienJson;
import com.loupsolitaire.backend.dto.ObjetJson;
import com.loupsolitaire.backend.dto.RefJson;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * DATA-19 : les fichiers data/en/*.json reprennent ceux de data/ avec le
 * texte officiel de la VO (Project Aon). Seuls les textes different : memes
 * entrees, dans le meme ordre, memes donnees de jeu.
 */
class DonneesAnglaisesTest {

    private static <T> List<T> lire(String chemin, Class<T> type) throws Exception {
        try (InputStream is = DonneesAnglaisesTest.class.getResourceAsStream(chemin)) {
            return JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .build().readerForListOf(type).readValue(is);
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

    @Test
    void chapitresMemesDonneesDeJeuQueLeFrancais() throws Exception {
        List<ChapitreJson> fr = lire("/data/chapitre.json", ChapitreJson.class);
        List<ChapitreJson> en = lire("/data/en/chapitre.json", ChapitreJson.class);

        assertThat(en).hasSameSizeAs(fr);
        for (int i = 0; i < fr.size(); i++) {
            assertThat(en.get(i)).as("chapitre %d", fr.get(i).getId())
                    .usingRecursiveComparison().ignoringFields("text").isEqualTo(fr.get(i));
        }
    }

    @Test
    void chapitresTexteCoherentAvecLesDonneesDeJeu() throws Exception {
        for (ChapitreJson c : lire("/data/en/chapitre.json", ChapitreJson.class)) {
            assertThat(c.getText()).as("chapitre %d", c.getId()).isNotBlank();
            // La carte de l'ennemi s'affiche a la place de ce jeton (chapitre.page.ts).
            assertThat(c.getText().contains("[[ENNEMI]]")).as("chapitre %d", c.getId()).isEqualTo(c.isCombat());
            // Chaque renvoi du livre est dans le texte ; les liens propres a l'app
            // (introduction -> 1, retour par Piece Premium, sections 2101/2102) n'y sont pas.
            for (LienJson lien : DonneesDuLivre.liens(c)) {
                boolean renvoiDuLivre = c.getId() >= 1 && c.getId() <= 350
                        && Integer.parseInt(lien.getPage()) <= 350
                        && DonneesDuLivre.conditions(lien).stream().noneMatch(cond -> "coin".equals(cond.getTargetId()));
                if (renvoiDuLivre) {
                    assertThat(c.getText()).as("chapitre %d -> %s", c.getId(), lien.getPage())
                            .contains("<strong>" + lien.getPage() + "</strong>");
                }
            }
        }
    }
}
