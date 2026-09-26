package com.loupsolitaire.backend.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.LienJson;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture de chapitre.json pour les tests de donnees (comme GameDataLoader),
 * sans Spring ni base.
 */
final class DonneesDuLivre {

    private static Map<Integer, ChapitreJson> chapitres;

    private DonneesDuLivre() {
    }

    static synchronized Map<Integer, ChapitreJson> chapitres() {
        if (chapitres == null) {
            JsonMapper mapper = JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .build();
            try (InputStream is = DonneesDuLivre.class.getResourceAsStream("/data/chapitre.json")) {
                List<ChapitreJson> liste = mapper.readerForListOf(ChapitreJson.class).readValue(is);
                chapitres = liste.stream().collect(Collectors.toMap(
                        ChapitreJson::getId, Function.identity(), (a, b) -> a, TreeMap::new));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return chapitres;
    }

    static List<LienJson> liens(ChapitreJson chapitre) {
        return chapitre.getLien() == null ? List.of() : chapitre.getLien();
    }

    static List<CondJson> conditions(LienJson lien) {
        return lien.getCond() == null ? List.of() : lien.getCond();
    }

    static boolean aUneCondition(LienJson lien, String type) {
        return conditions(lien).stream().anyMatch(c -> type.equalsIgnoreCase(c.getType()));
    }
}