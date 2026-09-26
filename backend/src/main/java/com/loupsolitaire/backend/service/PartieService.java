package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.TirageCreation;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.ChapitreParcouruResponse;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.TirageResponse;
import com.loupsolitaire.backend.service.mapper.ChapitreMapper;
import com.loupsolitaire.backend.service.mapper.CombatMapper;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;
import com.loupsolitaire.backend.service.record.TourJoue;

import lombok.RequiredArgsConstructor;

/**
 * Point d'entree unique des actions de jeu (PersonnageController,
 * CombatController).
 *
 * Chaque methode correspond a une requete HTTP et :
 * 1. ouvre LA transaction de la requete (les controleurs n'en ouvrent plus) ;
 * 2. charge le personnage a partir de son id et verifie qu'il appartient a
 *    l'utilisateur connecte ;
 * 3. appelle les services metier (PersonnageService, CombatService...) ;
 * 4. construit la reponse AVANT la fin de la transaction, pendant que les
 *    relations paresseuses sont encore accessibles.
 *
 * Le personnage reste "attache" a la transaction du debut a la fin : les
 * services metier le modifient directement, et Hibernate enregistre tout
 * seul les changements a la validation (plus besoin de save() a chaque
 * etape).
 *
 * Toute action qui MODIFIE le personnage le charge via
 * findByIdPourModification (verrou optimiste force, voir Personnage.version) ;
 * les lectures utilisent findById.
 */
@Service
@RequiredArgsConstructor
public class PartieService {

    private final PersonnageRepository personnageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjetRepository objetRepository;

    private final PersonnageService personnageService;
    private final InventaireService inventaireService;
    private final ObjetService objetService;
    private final EffetChapitreService effetChapitreService;
    private final CombatService combatService;
    private final JournalService journalService;
    private final TirageCreationService tirageCreationService;

    private final PersonnageMapper personnageMapper;
    private final ChapitreMapper chapitreMapper;
    private final CombatMapper combatMapper;

    // =========================================================
    // Personnages
    // =========================================================

    // Tirage des caracteristiques, fait par le serveur (SEC-01). Le meme
    // tirage est renvoye tant qu'aucun personnage n'a ete cree avec lui : le
    // joueur ne peut pas relancer les des, meme en rechargeant l'ecran.
    @Transactional
    public TirageResponse tirerCaracteristiques(UtilisateurConnecte connecte) {
        Utilisateur utilisateur = recupererUtilisateurVerrouille(connecte);
        return versReponse(tirageCreationService.tirerOuRelire(utilisateur.getId()));
    }

    // Les chiffres d'HABILETE et d'ENDURANCE viennent du tirage en attente
    // (POST /personnages/tirage), jamais de la requete du client.
    @Transactional
    public PersonnageResponse creerPersonnage(UtilisateurConnecte connecte, String nom,
                                              List<IdDiscipline> disciplines) {
        Utilisateur utilisateur = recupererUtilisateurVerrouille(connecte);
        TirageCreation tirage = tirageCreationService.consommer(utilisateur.getId());
        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, nom, disciplines, tirage.getHasardHabilite(), tirage.getHasardEndurance());
        return personnageMapper.versReponse(personnage);
    }

    // Ecran profil/accueil : triee par derniereActivite decroissante.
    // AccueilPage affiche toujours le premier element comme "a reprendre",
    // donc le personnage le plus recemment joue doit arriver en tete.
    @Transactional(readOnly = true)
    public List<PersonnageResponse> listerPersonnages(UtilisateurConnecte connecte) {
        Utilisateur utilisateur = recupererUtilisateur(connecte);
        return personnageRepository.findByUtilisateur(utilisateur).stream()
                .sorted(Comparator.<Personnage, Instant>comparing(
                        p -> p.getDerniereActivite() != null ? p.getDerniereActivite() : Instant.MIN)
                        .reversed())
                .map(personnageMapper::versReponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonnageResponse recupererPersonnage(UUID id, UtilisateurConnecte connecte) {
        return personnageMapper.versReponse(pourLecture(id, connecte));
    }

    // Le tirage HASARD est fige depuis l'arrivee sur ce chapitre (voir
    // PersonnageService.avancerVersChapitre) : consulter cet ecran plusieurs
    // fois ne le fait jamais changer.
    @Transactional(readOnly = true)
    public ChapitreResponse chapitreCourant(UUID id, UtilisateurConnecte connecte) {
        Personnage personnage = pourLecture(id, connecte);
        return chapitreMapper.versReponse(personnage.getChapitreActuel().getId(), personnage);
    }

    @Transactional(readOnly = true)
    public List<ChapitreParcouruResponse> historique(UUID id, UtilisateurConnecte connecte) {
        return journalService.lister(pourLecture(id, connecte));
    }

    @Transactional
    public PersonnageResponse avancerVersChapitre(UUID id, Integer chapitreCibleId, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        personnageService.avancerVersChapitre(personnage, chapitreCibleId);
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse revenirApresDefaite(UUID id, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        personnageService.revenirApresDefaite(personnage);
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse ressusciter(UUID id, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        personnageService.ressusciter(personnage);
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse ramasserObjet(UUID id, String objetId, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        personnageService.ramasserObjetDuChapitre(personnage, recupererObjet(objetId));
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse retirerObjet(UUID id, String objetId, int quantite, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        inventaireService.retirerObjet(personnage, recupererObjet(objetId), quantite);
        return personnageMapper.versReponse(personnage);
    }

    // Applique l'effet PUIS retire 1 exemplaire de l'inventaire.
    //
    // REGLE-05 : interdit une fois le combat engage (au moins un assaut
    // joue) : un objet utilise en combat remplace l'attaque ou la defense et
    // subit la riposte de l'ennemi (action OBJET, CombatService.jouerObjet).
    // Avant le premier assaut, l'objet reste utilisable librement (ex.
    // Essence d'Alether bue avant le combat, comme dans le livre).
    @Transactional
    public PersonnageResponse consommerObjet(UUID id, String objetId, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        boolean combatEngage = combatService.combatActuel(personnage)
                .filter(combat -> combat.getStatut() == StatutCombat.EN_COURS && combat.getAssautsLivres() > 0)
                .isPresent();
        if (combatEngage) {
            throw new IllegalArgumentException(
                    "Un combat est en cours : utilisez l'objet comme action de combat (OBJET)");
        }
        Objet objet = recupererObjet(objetId);
        objetService.appliquerEffetsConsommation(personnage, objet);
        inventaireService.retirerObjet(personnage, objet, 1);
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse resoudreVol(UUID id, String objetId, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        effetChapitreService.resoudreVolEnAttente(personnage, recupererObjet(objetId));
        return personnageMapper.versReponse(personnage);
    }

    @Transactional
    public PersonnageResponse echangerObjet(UUID id, String objetAAjouterId, String objetARetirerId,
                                            UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        Objet objetAAjouter = recupererObjet(objetAAjouterId);
        Objet objetARetirer = recupererObjet(objetARetirerId);
        personnageService.echangerObjet(personnage, objetARetirer, objetAAjouter);
        return personnageMapper.versReponse(personnage);
    }

    // Lecture simple : la suppression verifie deja la version elle-meme
    // (DELETE ... WHERE version = ?), pas besoin de forcer un increment.
    @Transactional
    public void supprimerPersonnage(UUID id, UtilisateurConnecte connecte) {
        personnageService.supprimerPersonnage(pourLecture(id, connecte));
    }

    // =========================================================
    // Combat
    // =========================================================

    // Demarre le combat du chapitre courant, ou renvoie celui deja existant
    // (idempotent : rouvrir l'ecran ne relance pas le combat a zero).
    @Transactional
    public CombatResponse initierCombat(UUID id, UtilisateurConnecte connecte) {
        Combat combat = combatService.initierCombat(pourModification(id, connecte));
        return combatMapper.versReponse(combat);
    }

    // Ne cree rien : 404 si aucun combat n'a encore ete initie sur ce chapitre.
    @Transactional(readOnly = true)
    public CombatResponse combatActuel(UUID id, UtilisateurConnecte connecte) {
        Personnage personnage = pourLecture(id, connecte);
        Combat combat = combatService.combatActuel(personnage)
                .orElseThrow(() -> new RessourceNonTrouveeException(
                        "Aucun combat en cours sur le chapitre " + personnage.getChapitreActuel().getId()));
        return combatMapper.versReponse(combat);
    }

    // objetId n'est utilise que pour ActionCombat.OBJET (null sinon).
    @Transactional
    public CombatResponse jouerTour(UUID id, ActionCombat action, String objetId, UtilisateurConnecte connecte) {
        Personnage personnage = pourModification(id, connecte);
        Objet objet = objetId != null ? recupererObjet(objetId) : null;
        TourJoue tourJoue = combatService.jouerTour(personnage, action, objet);
        return combatMapper.versReponse(tourJoue.combat(), tourJoue.resultat());
    }

    // =========================================================
    // Chargement et verification du proprietaire
    // =========================================================

    private Personnage pourLecture(UUID id, UtilisateurConnecte connecte) {
        return verifierProprietaire(id, personnageRepository.findById(id), connecte);
    }

    // Protege contre deux requetes simultanees sur le meme personnage (voir
    // PersonnageRepository.findByIdPourModification).
    private Personnage pourModification(UUID id, UtilisateurConnecte connecte) {
        return verifierProprietaire(id, personnageRepository.findByIdPourModification(id), connecte);
    }

    private Personnage verifierProprietaire(UUID id, Optional<Personnage> trouve, UtilisateurConnecte connecte) {
        Personnage personnage = trouve
                .orElseThrow(() -> new RessourceNonTrouveeException("Personnage introuvable : " + id));

        // Comparaison d'identifiants : l'utilisateur du personnage est deja
        // charge avec lui, aucune requete supplementaire.
        if (!personnage.getUtilisateur().getId().equals(connecte.id())) {
            throw new AccesRefuseException("Ce personnage ne vous appartient pas");
        }

        return personnage;
    }

    private Objet recupererObjet(String objetId) {
        return objetRepository.findById(objetId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Objet introuvable : " + objetId));
    }

    // Verrouille la ligne de l'utilisateur jusqu'a la fin de la transaction :
    // tirage et creation d'un meme utilisateur passent l'un apres l'autre.
    private Utilisateur recupererUtilisateurVerrouille(UtilisateurConnecte connecte) {
        return utilisateurRepository.findByIdPourModification(connecte.id())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));
    }

    private static TirageResponse versReponse(TirageCreation tirage) {
        return new TirageResponse(
                tirage.getHasardHabilite(),
                tirage.getHasardEndurance(),
                TirageCreationService.BASE_HABILITE + tirage.getHasardHabilite(),
                TirageCreationService.BASE_ENDURANCE + tirage.getHasardEndurance());
    }

    private Utilisateur recupererUtilisateur(UtilisateurConnecte connecte) {
        return utilisateurRepository.findById(connecte.id())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));
    }
}