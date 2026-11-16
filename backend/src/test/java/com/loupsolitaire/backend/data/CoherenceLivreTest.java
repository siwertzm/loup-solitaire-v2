package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.DisciplineJson;
import com.loupsolitaire.backend.dto.EffetJson;
import com.loupsolitaire.backend.dto.LienJson;

import tools.jackson.databind.json.JsonMapper;

/**
 * Coherence de chapitre.json avec son propre texte : ce que le texte
 * annonce (renvois, tirages, pertes d'ENDURANCE), les donnees doivent le
 * faire. Detecte une faute de saisie (lien vers le mauvais chapitre, plage
 * de hasard qui se chevauche, perte oubliee) sans relire le livre.
 */
class CoherenceLivreTest {

    // Un renvoi = un numero en gras (« rendez-vous au <strong>42</strong> »),
    // ou « rendez-vous au 61 » sans gras (section 326).
    private static final Pattern RENVOI = Pattern.compile(
            "<strong>(\\d+)</strong>|[Rr]endez-vous (?:directement )?au (\\d+)\\b");
    private static final Pattern PERTE_ENDURANCE = Pattern.compile("[Pp]erdez (\\d+) points? d'ENDURANCE");
    private static final Pattern PLAGE = Pattern.compile("\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*]");

    // Chapitres dont les renvois ne suivent pas le texte, volontairement :
    private static final Map<Integer, String> RENVOIS_HORS_TEXTE = Map.of(
            0, "debut de l'aventure : lien vers le 1 sans renvoi dans le texte",
            350, "fin du livre : lien vers la conclusion (351)",
            21, "section decoupee en 21 / 2101 / 2102 (tirages successifs du marecage)",
            2101, "sous-section de 21",
            2102, "sous-section de 21");

    // Chapitres ou une partie des tirages ne mene a aucun lien (mort).
    private static final Set<Integer> TIRAGES_PARTIELS = Set.of(
            2102); // seul le 9 sauve du marecage ; les autres chiffres sont la mort

    @Test
    void lesLiensDeChaqueChapitreSontLesRenvoisDeSonTexte() {
        List<String> ecarts = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            if (RENVOIS_HORS_TEXTE.containsKey(chapitre.getId())) {
                continue;
            }
            Set<Integer> renvois = new TreeSet<>();
            Matcher m = RENVOI.matcher(chapitre.getText());
            while (m.find()) {
                renvois.add(Integer.valueOf(m.group(1) != null ? m.group(1) : m.group(2)));
            }
            Set<Integer> liens = new TreeSet<>();
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                // Les liens des coins (D-07, retour apres une mort) ne sont pas
                // dans le texte du livre.
                if (!estUnLienDeCoin(lien)) {
                    liens.add(Integer.valueOf(lien.getPage()));
                }
            }
            if (!renvois.equals(liens)) {
                ecarts.add(chapitre.getId() + " : texte " + renvois + ", liens " + liens);
            }
        }

        assertThat(ecarts).isEmpty();
    }

    @Test
    void lesTiragesDeHasardCouvrentZeroANeufSansChevauchement() {
        List<String> ecarts = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            int[] couverture = new int[10];
            boolean aDesTirages = false;
            for (LienJson lien : DonneesDuLivre.liens(chapitre)) {
                for (CondJson condition : DonneesDuLivre.conditions(lien)) {
                    if ("hasard".equalsIgnoreCase(condition.getType())) {
                        aDesTirages = true;
                        Matcher plage = PLAGE.matcher(condition.getValeur());
                        assertThat(plage.matches())
                                .as("section %d : plage %s", chapitre.getId(), condition.getValeur())
                                .isTrue();
                        for (int c = Integer.parseInt(plage.group(1)); c <= Integer.parseInt(plage.group(2)); c++) {
                            couverture[c]++;
                        }
                    }
                }
            }
            if (!aDesTirages) {
                continue;
            }
            for (int chiffre = 0; chiffre <= 9; chiffre++) {
                boolean attendu = couverture[chiffre] == 1
                        || (couverture[chiffre] == 0 && TIRAGES_PARTIELS.contains(chapitre.getId()));
                if (!attendu) {
                    ecarts.add("section " + chapitre.getId() + " : le " + chiffre + " mene a "
                            + couverture[chiffre] + " lien(s)");
                }
            }
        }

        assertThat(ecarts).isEmpty();
    }

    @Test
    void chaquePerteDEnduranceAnnonceeASonEffet() {
        List<String> ecarts = new ArrayList<>();
        for (ChapitreJson chapitre : DonneesDuLivre.chapitres().values()) {
            String texte = chapitre.getText().replaceAll("<[^>]+>", "");
            Matcher m = PERTE_ENDURANCE.matcher(texte);
            while (m.find()) {
                int perte = Integer.parseInt(m.group(1));
                if (!aLEffet(chapitre, perte)) {
                    ecarts.add("section " + chapitre.getId() + " : perte de " + perte + " sans effet");
                }
            }
        }

        assertThat(ecarts).isEmpty();
    }

    /**
     * Une perte « perdez N points d'ENDURANCE » est portee soit par un effet
     * ENDURANCE de -N, soit par un effet REPAS (le texte dit alors « prenez un
     * Repas, sinon vous perdez 3 points » : la perte est geree par le repas).
     */
    private static boolean aLEffet(ChapitreJson chapitre, int perte) {
        List<EffetJson> effets = chapitre.getEffet() == null ? List.of() : chapitre.getEffet();
        return effets.stream().anyMatch(e ->
                ("endurance".equalsIgnoreCase(e.getType()) && e.getValeur() != null && e.getValeur() == -perte)
                        || "repas".equalsIgnoreCase(e.getType()));
    }

    // DATA-17 : fautes de frappe corrigees, qui ne doivent pas revenir
    // (HABILETE et non HABILITE, comme QUAL-07 pour l'interface).
    private static final List<String> FAUTES_CONNUES = List.of(
            "HABILIT", "Sommer-lund", "au ras une", "l'échiné", "vore arme", ", Utilisez");

    @Test
    void aucuneFauteConnueDansLesTextes() throws IOException {
        List<String> textes = new ArrayList<>();
        DonneesDuLivre.chapitres().values().forEach(c -> textes.add(c.getId() + " : " + c.getText()));
        try (InputStream is = CoherenceLivreTest.class.getResourceAsStream("/data/discipline.json")) {
            JsonMapper.builder().build().readerForListOf(DisciplineJson.class).<List<DisciplineJson>>readValue(is)
                    .forEach(d -> textes.add(d.getId() + " : " + d.getDescription()));
        }

        List<String> ecarts = new ArrayList<>();
        for (String texte : textes) {
            FAUTES_CONNUES.stream().filter(texte::contains)
                    .forEach(faute -> ecarts.add(texte.substring(0, texte.indexOf(" : ")) + " contient « " + faute + " »"));
        }

        assertThat(ecarts).isEmpty();
        assertThat(DonneesDuLivre.chapitres().get(340).getText()).startsWith("Vous galopez");
    }

    private static boolean estUnLienDeCoin(LienJson lien) {
        return DonneesDuLivre.conditions(lien).stream()
                .anyMatch(c -> "objet".equalsIgnoreCase(c.getType()) && "coin".equals(c.getTargetId()));
    }
}