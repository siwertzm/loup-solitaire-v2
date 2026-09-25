package com.loupsolitaire.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.EnnemiJson;
import com.loupsolitaire.backend.dto.RefJson;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Les stats des ennemis de chapitre.json + ennemi.json doivent correspondre
 * au livre, section par section (texte Gallimard et Project Aon, identiques
 * sur ces chiffres). Un ennemi reutilise dans plusieurs sections doit avoir
 * les memes stats partout : c'est ce qui avait masque les erreurs des
 * sections 220, 229 et 340 (DATA-01 a DATA-04).
 *
 * Test JUnit pur, sans Spring ni base : il lit les JSON du classpath comme
 * GameDataLoader.
 */
class StatsEnnemisConformesAuLivreTest {

    private record Stats(int habilite, int endurance) {
    }

    private static Stats s(int habilite, int endurance) {
        return new Stats(habilite, endurance);
    }

    // Section -> ennemis dans l'ordre du livre (HABILETE, ENDURANCE).
    private static final Map<Integer, List<Stats>> LIVRE = new TreeMap<>(Map.ofEntries(
            Map.entry(17, List.of(s(16, 24))),
            Map.entry(29, List.of(s(17, 25))),
            Map.entry(34, List.of(s(17, 25))),
            Map.entry(43, List.of(s(16, 10))),
            Map.entry(55, List.of(s(9, 9))),
            Map.entry(63, List.of(s(11, 10))),
            Map.entry(72, List.of(s(15, 24))),
            Map.entry(112, List.of(s(13, 10), s(12, 10))),
            Map.entry(133, List.of(s(16, 18))),
            Map.entry(136, List.of(s(13, 10), s(12, 10))),
            Map.entry(138, List.of(s(13, 10), s(12, 10))),
            Map.entry(169, List.of(s(16, 16))),
            Map.entry(170, List.of(s(17, 7))),
            Map.entry(180, List.of(s(15, 22), s(13, 20), s(12, 20))),
            Map.entry(191, List.of(s(11, 21))),
            Map.entry(208, List.of(s(15, 13))),
            Map.entry(220, List.of(s(11, 20))),
            Map.entry(227, List.of(s(16, 6))),
            Map.entry(229, List.of(s(16, 25))),
            Map.entry(231, List.of(s(13, 20))),
            Map.entry(246, List.of(s(15, 23))),
            Map.entry(253, List.of(s(13, 24), s(14, 23), s(14, 22), s(15, 21))),
            Map.entry(255, List.of(s(20, 30))),
            Map.entry(260, List.of(s(11, 18), s(12, 17))),
            Map.entry(283, List.of(s(17, 25))),
            Map.entry(336, List.of(s(14, 11), s(13, 11))),
            Map.entry(339, List.of(s(13, 20))),
            Map.entry(340, List.of(s(14, 24))),
            Map.entry(342, List.of(s(18, 26)))));

    // Sections ou le livre declare l'ennemi insensible a la Puissance
    // Psychique (133, 170, 255, 342) et a la Communication Animale (170).
    private static final Map<Integer, Set<String>> IMMUNITES = Map.of(
            133, Set.of("puissance_psychique"),
            170, Set.of("puissance_psychique", "communication_animale"),
            255, Set.of("puissance_psychique"),
            342, Set.of("puissance_psychique"));

    private static Map<Integer, ChapitreJson> chapitres;
    private static Map<String, EnnemiJson> ennemis;

    @BeforeAll
    static void chargerLesDonnees() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();
        chapitres = lire(mapper, "/data/chapitre.json", ChapitreJson.class).stream()
                .collect(Collectors.toMap(ChapitreJson::getId, Function.identity()));
        ennemis = lire(mapper, "/data/ennemi.json", EnnemiJson.class).stream()
                .collect(Collectors.toMap(EnnemiJson::getId, Function.identity()));
    }

    private static <T> List<T> lire(JsonMapper mapper, String chemin, Class<T> type) throws Exception {
        try (InputStream is = StatsEnnemisConformesAuLivreTest.class.getResourceAsStream(chemin)) {
            return mapper.readerForListOf(type).readValue(is);
        }
    }

    private static List<EnnemiJson> ennemisDe(int section) {
        List<RefJson> refs = chapitres.get(section).getEnnemi();
        return refs == null ? List.of() : refs.stream().map(ref -> ennemis.get(ref.getId())).toList();
    }

    @Test
    void lesSectionsDeCombatSontExactementCellesDuLivre() {
        Set<Integer> combats = chapitres.values().stream()
                .filter(ChapitreJson::isCombat)
                .map(ChapitreJson::getId)
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(combats).containsExactlyElementsOf(LIVRE.keySet());
    }

    @Test
    void chaqueSectionReferenceDesEnnemisExistants() {
        chapitres.values().forEach(chapitre -> {
            if (chapitre.getEnnemi() != null) {
                chapitre.getEnnemi().forEach(ref -> assertThat(ennemis)
                        .as("section %d", chapitre.getId())
                        .containsKey(ref.getId()));
            }
        });
    }

    @Test
    void lesStatsDeChaqueCombatSontCellesDuLivre() {
        LIVRE.forEach((section, attendu) -> {
            List<Stats> reel = ennemisDe(section).stream()
                    .map(e -> s(e.getHabilite(), e.getEndurance()))
                    .toList();
            assertThat(reel).as("section %d", section).containsExactlyElementsOf(attendu);
        });
    }

    @Test
    void lesImmunitesSontCellesDuLivre() {
        LIVRE.keySet().forEach(section -> {
            Set<String> reel = ennemisDe(section).stream()
                    .flatMap(e -> e.getResistance() == null ? java.util.stream.Stream.<RefJson>empty()
                            : e.getResistance().stream())
                    .map(RefJson::getId)
                    .collect(Collectors.toSet());
            assertThat(reel).as("section %d", section)
                    .isEqualTo(IMMUNITES.getOrDefault(section, Set.of()));
        });
    }
}