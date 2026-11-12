package com.loupsolitaire.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import com.loupsolitaire.backend.dto.ChapitreJson;
import com.loupsolitaire.backend.dto.CondJson;
import com.loupsolitaire.backend.dto.DisciplineJson;
import com.loupsolitaire.backend.dto.EffetJson;
import com.loupsolitaire.backend.dto.EnnemiJson;
import com.loupsolitaire.backend.dto.LienJson;
import com.loupsolitaire.backend.dto.ObjetChapJson;
import com.loupsolitaire.backend.dto.ObjetJson;
import com.loupsolitaire.backend.dto.RefJson;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.CatalogueEmpreinte;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.ObjetChap;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.CatalogueEmpreinteRepository;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.EnnemiRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Charge le contenu du livre (disciplines, objets, ennemis, chapitres) au
// demarrage.
//
// DATA-00 : les JSON font foi. Leur empreinte SHA-256 est gardee en base
// (catalogue_empreinte) ; tant qu'elle ne change pas, rien n'est recharge.
// Si elle change (ou sur une base neuve), tout le catalogue est remis a
// l'identique des JSON : mise a jour par identifiant, et enfants (liens,
// conditions, effets, objets et ennemis des chapitres, resistances)
// supprimes puis recrees. Possible car les donnees des joueurs ne
// referencent que des identifiants stables (chapitre, objet, ennemi,
// discipline). Corriger le livre = modifier le JSON, sans migration.
//
// OPS-03 : tout se fait dans UNE transaction, verifiee avant la fin
// (verifierIntegrite) ; un echec annule tout et l'empreinte n'est pas
// enregistree : le chargement complet reprend au demarrage suivant.
@Component
@RequiredArgsConstructor
@Slf4j
public class GameDataLoader implements ApplicationRunner {

    // Numeros de "page" utilises dans chapitre.json pour signifier une fin
    // de partie ou de tome (mort ou victoire finale) plutot qu'un vrai
    // chapitre. 18 occurrences au total dans ce tome, aucune ne correspond
    // a un veritable chapitre a atteindre (voir analyse menee avec l'utilisateur).
    private static final Set<String> PAGES_FIN_DE_JEU = Set.of("0", "351", "352");

    // Seul chapitre sans aucun lien jouable : la fin victorieuse du tome.
    static final int CHAPITRE_FIN_DU_TOME = 350;

    // Ordre fixe : il entre dans le calcul de l'empreinte.
    private static final List<String> FICHIERS_DU_LIVRE = List.of(
            "data/discipline.json", "data/ennemi.json", "data/objet.json", "data/chapitre.json");

    private final DisciplineRepository disciplineRepository;
    private final EnnemiRepository ennemiRepository;
    private final ObjetRepository objetRepository;
    private final ChapitreRepository chapitreRepository;
    private final CatalogueEmpreinteRepository catalogueEmpreinteRepository;
    // Jackson 3 a change deux valeurs par defaut par rapport a Jackson 2.
    // On garde explicitement l'ancien comportement pour les fichiers de donnees :
    // - un champ inconnu dans un JSON fait echouer le chargement (detecte les fautes de frappe) ;
    // - un null sur un champ primitif (int, boolean) donne 0 / false au lieu d'une erreur.
    private final JsonMapper objectMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        String empreinte = calculerEmpreinte();
        Optional<CatalogueEmpreinte> enBase = catalogueEmpreinteRepository.findById(CatalogueEmpreinte.ID_UNIQUE);

        if (enBase.map(CatalogueEmpreinte::getEmpreinte).filter(empreinte::equals).isPresent()) {
            log.info("Catalogue du livre a jour (empreinte {}), aucun rechargement.", empreinte.substring(0, 12));
            return;
        }

        log.info("Catalogue du livre absent ou modifie (empreinte {}) : rechargement depuis les JSON.",
                empreinte.substring(0, 12));
        chargerDisciplines();
        chargerEnnemis();
        chargerObjets();
        chargerChapitres();

        CatalogueEmpreinte ligne = enBase.orElseGet(CatalogueEmpreinte::new);
        ligne.setEmpreinte(empreinte);
        ligne.setChargeLe(Instant.now());
        catalogueEmpreinteRepository.save(ligne);
    }

    // SHA-256 des quatre fichiers, dans un ordre fixe.
    static String calculerEmpreinte() throws IOException {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est obligatoire dans toute JVM : ne peut pas arriver.
            throw new IllegalStateException(e);
        }
        for (String fichier : FICHIERS_DU_LIVRE) {
            try (InputStream is = new ClassPathResource(fichier).getInputStream()) {
                sha256.update(is.readAllBytes());
            }
            // Separateur : deplacer du texte d'un fichier a l'autre change l'empreinte.
            sha256.update((byte) 0);
        }
        return HexFormat.of().formatHex(sha256.digest());
    }

    private static <K, E> Map<K, E> parId(List<E> entites, Function<E, K> id) {
        return entites.stream().collect(Collectors.toMap(id, e -> e));
    }

    // Une entree retiree des JSON n'est jamais supprimee : un joueur peut
    // encore l'avoir dans son inventaire, son combat ou son parcours.
    private static <K> void signalerRetires(String type, Set<K> enBase, Set<K> dansLesJson) {
        Set<String> retires = new TreeSet<>();
        enBase.stream().filter(id -> !dansLesJson.contains(id)).forEach(id -> retires.add(String.valueOf(id)));
        if (!retires.isEmpty()) {
            log.warn("{} absents des JSON, gardes en base : {}", type, retires);
        }
    }

    private void chargerDisciplines() throws Exception {
        List<DisciplineJson> donnees = lireJson("data/discipline.json", DisciplineJson.class);
        Map<IdDiscipline, Discipline> existantes = parId(disciplineRepository.findAll(), Discipline::getId);

        List<Discipline> disciplines = donnees.stream()
                .map(json -> {
                    IdDiscipline id = IdDiscipline.fromJson(json.getId());
                    Discipline discipline = existantes.getOrDefault(id, new Discipline());
                    discipline.setId(id);
                    discipline.setNom(json.getNom());
                    discipline.setDescription(json.getDescription());
                    return discipline;
                })
                .toList();

        disciplineRepository.saveAll(disciplines);
        signalerRetires("Disciplines", existantes.keySet(),
                disciplines.stream().map(Discipline::getId).collect(Collectors.toSet()));
        log.info("✅ {} disciplines chargees", disciplines.size());
    }

    private void chargerEnnemis() throws Exception {
        List<EnnemiJson> donnees = lireJson("data/ennemi.json", EnnemiJson.class);
        Map<String, Ennemi> existants = parId(ennemiRepository.findAll(), Ennemi::getId);

        List<Ennemi> ennemis = donnees.stream()
                .map(json -> versEntite(json, existants.getOrDefault(json.getId(), new Ennemi())))
                .toList();

        ennemiRepository.saveAll(ennemis);
        signalerRetires("Ennemis", existants.keySet(),
                ennemis.stream().map(Ennemi::getId).collect(Collectors.toSet()));
        log.info("✅ {} ennemis charges", ennemis.size());
    }

    private Ennemi versEntite(EnnemiJson json, Ennemi ennemi) {
        ennemi.setId(json.getId());
        ennemi.setNom(json.getNom());
        ennemi.setDescription(json.getDescription());
        ennemi.setHabilite(json.getHabilite());
        ennemi.setEndurance(json.getEndurance());

        // Collections videes puis remplies (jamais remplacees : Hibernate
        // suit l'instance qu'il a chargee).
        ennemi.getResistances().clear();
        if (json.getResistance() != null) {
            ennemi.getResistances().addAll(disciplinesEnnemi(json.getId(), json.getResistance()));
        }
        ennemi.getDisciplines().clear();
        if (json.getDisciplines() != null) {
            ennemi.getDisciplines().addAll(disciplinesEnnemi(json.getId(), json.getDisciplines()));
        }

        return ennemi;
    }

    private List<Discipline> disciplinesEnnemi(String ennemiId, List<RefJson> refs) {
        return refs.stream()
                .map(RefJson::getId)
                .map(IdDiscipline::fromJson)
                .map(id -> disciplineRepository.findById(id)
                        .orElseThrow(() -> new RessourceNonTrouveeException(
                                "Discipline introuvable pour l'ennemi " + ennemiId + " : " + id)))
                .toList();
    }

    private <T> List<T> lireJson(String cheminClasspath, Class<T> type) throws Exception {
        try (InputStream is = new ClassPathResource(cheminClasspath).getInputStream()) {
            return objectMapper.readerForListOf(type).readValue(is);
        }
    }

    private void chargerObjets() throws Exception {
        List<ObjetJson> donnees = lireJson("data/objet.json", ObjetJson.class);
        Map<String, Objet> existants = parId(objetRepository.findAll(), Objet::getId);

        // Anciens effets supprimes en base AVANT de creer les nouveaux.
        existants.values().forEach(objet -> objet.getEffets().clear());
        objetRepository.flush();

        // Les objets ne sont pas encore en base : les conditions de leurs
        // effets sont verifiees par rapport au contenu du fichier lui-meme.
        Set<String> idsObjets = donnees.stream().map(ObjetJson::getId).collect(Collectors.toSet());

        List<Objet> objets = donnees.stream()
                .map(json -> versEntite(json, existants.getOrDefault(json.getId(), new Objet()), idsObjets::contains))
                .toList();

        objetRepository.saveAll(objets);
        signalerRetires("Objets", existants.keySet(), idsObjets);
        log.info("✅ {} objets charges", objets.size());
    }

    private Objet versEntite(ObjetJson json, Objet objet, Predicate<String> objetExiste) {
        objet.setId(json.getId());
        objet.setNom(json.getNom());
        objet.setDescription(json.getDescription());
        objet.setCategorie(CategorieObjet.fromJson(json.getCategorie()));

        if (json.getEffet() != null) {
            List<Effet> effets = json.getEffet().stream()
                    .map(effetJson -> versEntite(effetJson, objet, objetExiste))
                    .toList();
            objet.getEffets().addAll(effets);
        }

        return objet;
    }

    // objet et chapitre sont mutuellement exclusifs : un seul est non-null
    // selon le contexte d'appel (voir doc de conception, Effet).
    private Effet versEntite(EffetJson json, Objet objetParent, Predicate<String> objetExiste) {
        Effet effet = new Effet();
        effet.setType(TypeEffet.fromJson(json.getType()));
        effet.setValeur(json.getValeur());
        effet.setNom(json.getNom());
        effet.setObjet(objetParent);

        if (json.getCond() != null) {
            String origine = "effet de l'objet " + objetParent.getId();
            List<Cond> conditions = json.getCond().stream()
                    .map(condJson -> {
                        Cond cond = versEntite(condJson, origine, objetExiste);
                        cond.setEffet(effet);
                        return cond;
                    })
                    .toList();
            effet.setConditions(conditions);
        }

        return effet;
    }


    private void chargerChapitres() throws Exception {
        List<ChapitreJson> donnees = lireJson("data/chapitre.json", ChapitreJson.class);
        Map<Integer, Chapitre> existants = parId(chapitreRepository.findAll(), Chapitre::getId);

        // --- PASSE 1 : tous les chapitres "nus" (id + texte + combat) ---
        // Necessaire avant la passe 2 car un Lien.chapitreCible doit toujours
        // pointer vers un Chapitre deja existant en base. Les enfants des
        // chapitres existants sont supprimes ici, en base, avant d'etre
        // recrees en passe 2.
        List<Chapitre> chapitresNus = donnees.stream()
                .map(json -> {
                    Chapitre chapitre = existants.getOrDefault(json.getId(), new Chapitre());
                    chapitre.setId(json.getId());
                    chapitre.setText(json.getText());
                    chapitre.setCombat(json.isCombat());
                    chapitre.getEnnemis().clear();
                    chapitre.getEffets().clear();
                    chapitre.getLiens().clear();
                    chapitre.getObjets().clear();
                    return chapitre;
                })
                .toList();
        chapitreRepository.saveAll(chapitresNus);
        chapitreRepository.flush();

        Map<Integer, Chapitre> chapitresParId = chapitresNus.stream()
                .collect(Collectors.toMap(Chapitre::getId, c -> c));

        // --- PASSE 2 : attacher ennemis, effets, liens, objets ---
        int liensIgnores = 0;
        for (ChapitreJson json : donnees) {
            Chapitre chapitre = chapitresParId.get(json.getId());

            if (json.getEnnemi() != null) {
                List<Ennemi> ennemis = json.getEnnemi().stream()
                        .map(ref -> ennemiRepository.findById(ref.getId())
                                .orElseThrow(() -> new RessourceNonTrouveeException(
                                        "Ennemi introuvable pour le chapitre " + json.getId() + " : " + ref.getId())))
                        .toList();
                chapitre.getEnnemis().addAll(ennemis);
            }

            if (json.getEffet() != null) {
                List<Effet> effets = json.getEffet().stream()
                        .map(effetJson -> versEntite(effetJson, chapitre))
                        .toList();
                chapitre.getEffets().addAll(effets);
            }

            if (json.getLien() != null) {
                List<Lien> liens = new ArrayList<>();
                for (LienJson lienJson : json.getLien()) {
                    if (PAGES_FIN_DE_JEU.contains(lienJson.getPage())) {
                        liensIgnores++;
                        continue;
                    }
                    liens.add(versEntite(lienJson, chapitre, chapitresParId, json.getId()));
                }
                chapitre.getLiens().addAll(liens);
            }

            if (json.getObjet() != null) {
                List<ObjetChap> objets = json.getObjet().stream()
                        .map(objetChapJson -> versEntite(objetChapJson, chapitre, json.getId()))
                        .toList();
                chapitre.getObjets().addAll(objets);
            }
        }

        verifierIntegrite(chapitresParId.values());
        chapitreRepository.saveAll(chapitresParId.values());
        signalerRetires("Chapitres", existants.keySet(), chapitresParId.keySet());
        log.info("✅ {} chapitres charges ({} liens de fin de partie/tome ignores)",
                chapitresParId.size(), liensIgnores);
    }

    // OPS-03 : un chapitre sans issue bloquerait le joueur. Seule la fin du
    // tome n'a aucun lien jouable (ses sorties vers 351 sont ignorees).
    static void verifierIntegrite(Collection<Chapitre> chapitres) {
        List<Integer> sansLien = chapitres.stream()
                .filter(c -> c.getId() != CHAPITRE_FIN_DU_TOME && c.getLiens().isEmpty())
                .map(Chapitre::getId)
                .sorted()
                .toList();
        if (!sansLien.isEmpty()) {
            throw new IllegalStateException("Catalogue incoherent, chapitres sans lien : " + sansLien);
        }
    }

    // objet et chapitre sont mutuellement exclusifs : un seul est non-null
    // selon le contexte d'appel (voir doc de conception, Effet).
    private Effet versEntite(EffetJson json, Chapitre chapitreParent) {
        Effet effet = new Effet();
        effet.setType(TypeEffet.fromJson(json.getType()));
        effet.setValeur(json.getValeur());
        effet.setNom(json.getNom());
        effet.setChapitre(chapitreParent);

        if (json.getCond() != null) {
            String origine = "effet du chapitre " + chapitreParent.getId();
            List<Cond> conditions = json.getCond().stream()
                    .map(condJson -> {
                        Cond cond = versEntite(condJson, origine, this::objetEnBase);
                        cond.setEffet(effet);
                        return cond;
                    })
                    .toList();
            effet.setConditions(conditions);
        }

        return effet;
    }

    private Lien versEntite(LienJson json, Chapitre chapitreSource,
                             Map<Integer, Chapitre> chapitresParId, int chapitreSourceId) {
        int idCible = Integer.parseInt(json.getPage());
        Chapitre cible = chapitresParId.get(idCible);
        if (cible == null) {
            throw new RessourceNonTrouveeException(
                    "Chapitre cible introuvable pour le lien du chapitre " + chapitreSourceId + " : " + idCible);
        }

        Lien lien = new Lien();
        lien.setChapitre(chapitreSource);
        lien.setChapitreCible(cible);

        if (json.getCond() != null) {
            String origine = "lien du chapitre " + chapitreSourceId + " vers " + idCible;
            List<Cond> conditions = json.getCond().stream()
                    .map(condJson -> {
                        Cond cond = versEntite(condJson, origine, this::objetEnBase);
                        cond.setLien(lien);
                        return cond;
                    })
                    .toList();
            lien.setConditions(conditions);
        }

        return lien;
    }

    // Cree une condition (d'effet ou de lien) et VERIFIE ses donnees : une
    // faute de frappe dans un JSON du livre fait echouer le demarrage avec
    // un message precis, au lieu de donner en jeu une condition toujours
    // vraie ou toujours fausse sans que personne ne s'en apercoive.
    private Cond versEntite(CondJson json, String origine, Predicate<String> objetExiste) {
        try {
            Cond cond = new Cond();
            cond.setType(TypeCondition.fromJson(json.getType()));
            cond.setTargetId(json.getTargetId());
            cond.setValeur(json.getValeur());
            validerCondition(cond, objetExiste);
            return cond;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new IllegalStateException("Condition invalide (" + origine + ") : " + e.getMessage(), e);
        }
    }

    // Verifie ce que chaque type de condition utilise reellement en jeu
    // (voir ConditionService, CombatService et EffetChapitreService).
    // Visible dans le package pour les tests.
    static void validerCondition(Cond cond, Predicate<String> objetExiste) {
        switch (cond.getType()) {
            // "[min, max]" entre 0 et 9.
            case HASARD -> cond.plageHasard();
            // Marqueur sans valeur ; la valeur de VICTOIRE n'est pas lue.
            case PERMANENT, VICTOIRE, SANS_ARME -> {
            }
            // 1 = possede, -1 = ne possede PAS (voir EffetChapitreService).
            case DISCIPLINE -> {
                cond.valeurEntiere();
                IdDiscipline.fromJson(cond.getTargetId());
            }
            case OBJET, BOURSE -> {
                cond.valeurEntiere();
                verifierObjet(cond.getTargetId(), objetExiste);
            }
            // Sans targetId : "une arme quelconque" (ex. vol d'arme, chapitre 274).
            case ARME -> {
                cond.valeurEntiere();
                if (cond.getTargetId() != null) {
                    verifierObjet(cond.getTargetId(), objetExiste);
                }
            }
            // Seuils numeriques (endurance, nombre d'assauts, 1/0...).
            case ENDURANCE, ENDURANCE_INF, FUITE, ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE -> cond.valeurEntiere();
        }
    }

    private static void verifierObjet(String objetId, Predicate<String> objetExiste) {
        if (objetId == null) {
            throw new IllegalStateException("Objet cible manquant");
        }
        if (!objetExiste.test(objetId)) {
            throw new IllegalStateException("Objet inconnu : " + objetId);
        }
    }

    private boolean objetEnBase(String objetId) {
        return objetRepository.findById(objetId).isPresent();
    }

    private ObjetChap versEntite(ObjetChapJson json, Chapitre chapitreParent, int chapitreId) {
        Objet objet = objetRepository.findById(json.getId())
                .orElseThrow(() -> new RessourceNonTrouveeException(
                        "Objet introuvable pour le chapitre " + chapitreId + " : " + json.getId()));

        ObjetChap objetChap = new ObjetChap();
        objetChap.setChapitre(chapitreParent);
        objetChap.setObjet(objet);
        objetChap.setValeur(json.getValeur());
        objetChap.setOptionnel(json.isOptionnel());
        return objetChap;
    }
}