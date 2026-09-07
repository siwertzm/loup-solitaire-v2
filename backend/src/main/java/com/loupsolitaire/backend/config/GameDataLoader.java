package com.loupsolitaire.backend.config;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.DisciplineJson;
import com.loupsolitaire.backend.dto.EffetJson;
import com.loupsolitaire.backend.dto.EnnemiJson;
import com.loupsolitaire.backend.dto.ObjetJson;
import com.loupsolitaire.backend.dto.RefJson;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.EnnemiRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Charge le contenu du livre (disciplines, objets, ennemis, chapitres) au
// demarrage. Idempotent : si les disciplines sont deja en base (cas d'un
// redemarrage avec ddl-auto=update), on ne recharge rien.
@Component
@RequiredArgsConstructor
@Slf4j
public class GameDataLoader implements ApplicationRunner {

    private final DisciplineRepository disciplineRepository;
    private final EnnemiRepository ennemiRepository;
    private final ObjetRepository objetRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        chargerDisciplines();
        chargerEnnemis();
        chargerObjets();
    }

    private void chargerDisciplines() throws Exception {
        if (disciplineRepository.count() > 0) {
            log.info("Disciplines deja chargees, etape ignoree.");
            return;
        }

        List<DisciplineJson> donnees = lireJson("data/discipline.json", DisciplineJson.class);

        List<Discipline> disciplines = donnees.stream()
                .map(this::versEntite)
                .toList();

        disciplineRepository.saveAll(disciplines);
        log.info("✅ {} disciplines chargees", disciplines.size());
    }

    private Discipline versEntite(DisciplineJson json) {
        Discipline discipline = new Discipline();
        discipline.setId(IdDiscipline.fromJson(json.getId()));
        discipline.setNom(json.getNom());
        discipline.setDescription(json.getDescription());
        return discipline;
    }

    private void chargerEnnemis() throws Exception {
        if (ennemiRepository.count() > 0) {
            log.info("Ennemis deja charges, etape ignoree.");
            return;
        }

        List<EnnemiJson> donnees = lireJson("data/ennemi.json", EnnemiJson.class);

        List<Ennemi> ennemis = donnees.stream()
                .map(this::versEntite)
                .toList();

        ennemiRepository.saveAll(ennemis);
        log.info("✅ {} ennemis charges", ennemis.size());
    }

    private Ennemi versEntite(EnnemiJson json) {
        Ennemi ennemi = new Ennemi();
        ennemi.setId(json.getId());
        ennemi.setNom(json.getNom());
        ennemi.setDescription(json.getDescription());
        ennemi.setHabilite(json.getHabilite());
        ennemi.setEndurance(json.getEndurance());

        if (json.getResistance() != null) {
            List<Discipline> resistances = json.getResistance().stream()
                    .map(RefJson::getId)
                    .map(IdDiscipline::fromJson)
                    .map(id -> disciplineRepository.findById(id)
                            .orElseThrow(() -> new RessourceNonTrouveeException(
                                    "Discipline introuvable pour l'ennemi " + json.getId() + " : " + id)))
                    .toList();
            ennemi.setResistances(resistances);
        }

        return ennemi;
    }

    private <T> List<T> lireJson(String cheminClasspath, Class<T> type) throws Exception {
        try (InputStream is = new ClassPathResource(cheminClasspath).getInputStream()) {
            return objectMapper.readerForListOf(type).readValue(is);
        }
    }

    private void chargerObjets() throws Exception {
        if (objetRepository.count() > 0) {
            log.info("Objets deja charges, etape ignoree.");
            return;
        }

        List<ObjetJson> donnees = lireJson("data/objet.json", ObjetJson.class);

        List<Objet> objets = donnees.stream()
                .map(this::versEntite)
                .toList();

        objetRepository.saveAll(objets);
        log.info("✅ {} objets charges", objets.size());
    }

    private Objet versEntite(ObjetJson json) {
        Objet objet = new Objet();
        objet.setId(json.getId());
        objet.setNom(json.getNom());
        objet.setDescription(json.getDescription());
        objet.setCategorie(CategorieObjet.fromJson(json.getCategorie()));

        if (json.getEffet() != null) {
            List<Effet> effets = json.getEffet().stream()
                    .map(effetJson -> versEntite(effetJson, objet))
                    .toList();
            objet.setEffets(effets);
        }

        return objet;
    }

    // objet et chapitre sont mutuellement exclusifs : un seul est non-null
    // selon le contexte d'appel (voir doc de conception, Effet).
    private Effet versEntite(EffetJson json, Objet objetParent) {
        Effet effet = new Effet();
        effet.setType(TypeEffet.fromJson(json.getType()));
        effet.setValeur(json.getValeur());
        effet.setObjet(objetParent);

        if (json.getCond() != null) {
            List<Cond> conditions = json.getCond().stream()
                    .map(condJson -> versEntite(condJson, effet))
                    .toList();
            effet.setConditions(conditions);
        }

        return effet;
    }

    private Cond versEntite(CondJson json, Effet effetParent) {
        Cond cond = new Cond();
        cond.setType(TypeCondition.fromJson(json.getType()));
        cond.setTargetId(json.getTargetId());
        cond.setValeur(json.getValeur());
        cond.setEffet(effetParent);
        return cond;
    }
}