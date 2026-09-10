package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ResultatTour;
import com.loupsolitaire.backend.service.record.TourJoue;

import lombok.RequiredArgsConstructor;

// Orchestre un combat de bout en bout : creation a l'entree sur un Chapitre
// combat=true, puis un tour a la fois (ATTAQUE/DEFENSE/OBJET/FUITE). Server
// fait autorite sur l'issue de chaque tir (TableCombatService) : le client
// ne fait qu'indiquer l'action choisie, jamais un resultat.
//
// L'ordre des ennemis d'un meme chapitre est sequentiel ("a tour de role") :
// voir Combat.ennemiActifIndex. Une fois le dernier ennemi vaincu, le
// combat passe VICTOIRE ; le Personnage peut alors quitter le chapitre via
// PersonnageService.avancerVersChapitre (qui verifie ce statut).
@Service
@RequiredArgsConstructor
public class CombatService {

    private final CombatRepository combatRepository;
    private final ChapitreRepository chapitreRepository;
    private final PersonnageRepository personnageRepository;
    private final TableDeHasardService tableDeHasardService;
    private final TableCombatService tableCombatService;
    private final InventaireService inventaireService;
    private final ObjetService objetService;

    // Cree le combat du Chapitre courant du personnage, ou renvoie le
    // dernier existant (EN_COURS ou deja resolu) si l'ecran est
    // rafraichi/rouvert (idempotent).
    @Transactional
    public Combat initierCombat(Personnage personnage) {
        verifierPasMort(personnage);
        return combatEnCoursOuNouveau(personnage);
    }

    private Combat combatEnCoursOuNouveau(Personnage personnage) {
        Chapitre chapitre = recupererChapitre(personnage.getChapitreActuel().getId());
        if (!chapitre.isCombat()) {
            throw new IllegalArgumentException("Le chapitre " + chapitre.getId() + " n'est pas un combat");
        }

        // IMPORTANT : cherche le PLUS RECENT combat, quel que soit son
        // statut - pas seulement EN_COURS. Un combat deja resolu
        // (VICTOIRE/DEFAITE/FUITE/INTERROMPU) doit rester LE combat de ce
        // chapitre pour toujours ; le retrouver ici (au lieu de ne
        // chercher que EN_COURS) evite d'en recreer un nouveau à chaque
        // appel de POST /combat ou /combat/tour une fois le combat
        // termine, ce qui effacerait silencieusement son issue.
        return combatRepository
                .findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, chapitre.getId())
                .orElseGet(() -> creerCombat(personnage, chapitre));
    }

    private Combat creerCombat(Personnage personnage, Chapitre chapitre) {
        Combat combat = new Combat();
        combat.setPersonnage(personnage);
        combat.setChapitreId(chapitre.getId());
        combat.setEnnemiActifIndex(0);
        combat.setAssautsLivres(0);
        combat.setEndurancePerdue(false);
        combat.setBonusHabiliteEnAttente(0);
        combat.setStatut(StatutCombat.EN_COURS);
        combat.setCreeLe(Instant.now());

        List<CombatEnnemi> ennemis = new ArrayList<>();
        int ordre = 0;
        for (Ennemi ennemiCatalogue : chapitre.getEnnemis()) {
            CombatEnnemi ce = new CombatEnnemi();
            ce.setCombat(combat);
            ce.setEnnemi(ennemiCatalogue);
            ce.setOrdre(ordre++);
            ce.setEnduranceActuelle(ennemiCatalogue.getEndurance());
            ennemis.add(ce);
        }
        combat.setEnnemis(ennemis);

        return combatRepository.save(combat);
    }

    // Lecture seule : ne cree jamais de combat, contrairement a
    // initierCombat. Utilise par GET /combat (404 si aucun combat n'existe
    // encore sur ce chapitre plutot que d'en creer un a la volee).
    @Transactional(readOnly = true)
    public Optional<Combat> combatActuel(Personnage personnage) {
        return combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(
                personnage, personnage.getChapitreActuel().getId());
    }

    // Joue un tour. objetId n'est utilise (et requis) que pour ActionCombat.OBJET.
    //
    // Charge SON PROPRE Combat (via combatEnCoursOuNouveau) plutot que d'en
    // accepter un en parametre : un Combat charge par un appel PRECEDENT
    // (ex. initierCombat, dans le controleur) est detache une fois cette
    // transaction refermee. Le reattacher ici via combatRepository.save()
    // declencherait un merge() Hibernate qui recharge "ennemis.ennemi" SANS
    // le fetch-graph (perte du chargement pourtant deja fait), provoquant
    // une LazyInitializationException plus tard dans CombatMapper.
    @Transactional
    public TourJoue jouerTour(Personnage personnage, ActionCombat action, Objet objet) {
        verifierPasMort(personnage);
        Combat combat = combatEnCoursOuNouveau(personnage);

        if (combat.getStatut() != StatutCombat.EN_COURS) {
            // IllegalArgumentException (pas IllegalStateException) : c'est
            // le seul type d'exception "requete invalide" gere avec un 400
            // par GlobalExceptionHandler dans ce projet.
            throw new IllegalArgumentException("Ce combat est deja termine (" + combat.getStatut() + ")");
        }

        ResultatTour resultat = switch (action) {
            case ATTAQUE -> jouerAttaque(personnage, combat);
            case DEFENSE -> jouerDefense(personnage, combat);
            case OBJET -> jouerObjet(personnage, combat, objet);
            case FUITE -> jouerFuite(personnage, combat);
        };

        // ASSAUT_ECHEC : si le seuil du chapitre est desormais atteint sans
        // que l'ennemi actif soit mort, le combat s'arrete de force (texte
        // du livre, ex. chapitre 231). Ne s'applique qu'aux tours qui font
        // progresser les assauts (pas FUITE, deja gere a part) et seulement
        // si aucun autre denouement (DEFAITE...) n'a deja ete fixe.
        if (action != ActionCombat.FUITE && combat.getStatut() == StatutCombat.EN_COURS) {
            Chapitre chapitre = recupererChapitre(combat.getChapitreId());
            if (assautEchecAtteint(chapitre, combat)) {
                combat.setStatut(StatutCombat.INTERROMPU);
            }
        }

        personnageRepository.save(personnage);
        Combat sauvegarde = combatRepository.save(combat);
        return new TourJoue(sauvegarde, resultat);
    }

    private ResultatTour jouerAttaque(Personnage personnage, Combat combat) {
        CombatEnnemi ennemiActif = ennemiActifRequis(combat);
        Ennemi ennemi = ennemiActif.getEnnemi();
        int habEnnemi = ennemi.getHabilite();

        int bonusUtilise = combat.getBonusHabiliteEnAttente();
        int habJoueur = habiliteEffective(personnage, ennemi) + bonusUtilise;
        int rapportAttaque = habJoueur - habEnnemi;
        int tirageAttaque = tableDeHasardService.tirerChiffre();
        int degatsInfliges = tableCombatService.degatsInfliges(rapportAttaque, tirageAttaque);

        combat.setBonusHabiliteEnAttente(0);
        combat.setAssautsLivres(combat.getAssautsLivres() + 1);

        int nouvelleEnduranceEnnemi = Math.max(0, ennemiActif.getEnduranceActuelle() + degatsInfliges);
        ennemiActif.setEnduranceActuelle(nouvelleEnduranceEnnemi);

        if (nouvelleEnduranceEnnemi <= 0) {
            passerAuProchainEnnemi(combat);
            // Ennemi mort : pas de riposte ce tour.
            return new ResultatTour("ATTAQUE", rapportAttaque, tirageAttaque, degatsInfliges,
                    null, null, null, null, null, null, null);
        }

        // L'ennemi riposte : pas de bonus d'HABILITE sur la defense du
        // joueur ici (le bonus ne joue que sur SA propre attaque).
        int rapportRiposte = habiliteEffective(personnage, ennemi) - habEnnemi;
        int tirageRiposte = tableDeHasardService.tirerChiffre();
        int degatsSubis = tableCombatService.degatsSubis(rapportRiposte, tirageRiposte);
        appliquerDegatsAuJoueur(personnage, combat, degatsSubis);

        return new ResultatTour("ATTAQUE", rapportAttaque, tirageAttaque, degatsInfliges,
                rapportRiposte, tirageRiposte, degatsSubis, degatsSubis, null, null, null);
    }

    private ResultatTour jouerDefense(Personnage personnage, Combat combat) {
        CombatEnnemi ennemiActif = ennemiActifRequis(combat);
        Ennemi ennemi = ennemiActif.getEnnemi();
        int habEnnemi = ennemi.getHabilite();

        int tirageDefense = tableDeHasardService.tirerChiffre();
        int reduction = tableCombatService.reductionDefense(tirageDefense);
        int bonus = tableCombatService.bonusHabilite(tirageDefense);
        // Le bonus s'applique immediatement (attenue la riposte de ce tour)
        // ET reste disponible pour la PROCHAINE ATTAQUE (voir ActionCombat).
        combat.setBonusHabiliteEnAttente(bonus);
        combat.setAssautsLivres(combat.getAssautsLivres() + 1);

        int rapportRiposte = (habiliteEffective(personnage, ennemi) + bonus) - habEnnemi;
        int tirageRiposte = tableDeHasardService.tirerChiffre();
        int degatsBruts = tableCombatService.degatsSubis(rapportRiposte, tirageRiposte);
        int degatsReduits = (int) Math.round(degatsBruts * (100.0 - reduction) / 100.0);

        appliquerDegatsAuJoueur(personnage, combat, degatsReduits);

        return new ResultatTour("DEFENSE", null, null, null,
                rapportRiposte, tirageRiposte, degatsBruts, degatsReduits, tirageDefense, reduction, bonus);
    }

    private ResultatTour jouerObjet(Personnage personnage, Combat combat, Objet objet) {
        if (objet == null) {
            throw new IllegalArgumentException("Un objet est requis pour l'action OBJET");
        }
        if (objet.getCategorie() != CategorieObjet.OBJET) {
            throw new IllegalArgumentException("Cet objet ne peut pas etre consomme en combat : " + objet.getId());
        }

        CombatEnnemi ennemiActif = ennemiActifRequis(combat);
        Ennemi ennemi = ennemiActif.getEnnemi();
        int habEnnemi = ennemi.getHabilite();

        // Meme mecanique que POST /objets/{objetId}/consommer : applique
        // l'effet ENDURANCE/HABILETE de l'objet, puis le retire.
        objetService.appliquerEffetsConsommation(personnage, objet);
        inventaireService.retirerObjet(personnage, objet, 1);

        combat.setAssautsLivres(combat.getAssautsLivres() + 1);

        // Le bonus d'une DEFENSE precedente reste en reserve pour la
        // prochaine ATTAQUE : consommer un objet ne le consomme pas.
        int rapportRiposte = habiliteEffective(personnage, ennemi) - habEnnemi;
        int tirageRiposte = tableDeHasardService.tirerChiffre();
        int degatsSubis = tableCombatService.degatsSubis(rapportRiposte, tirageRiposte);
        appliquerDegatsAuJoueur(personnage, combat, degatsSubis);

        return new ResultatTour("OBJET", null, null, null,
                rapportRiposte, tirageRiposte, degatsSubis, degatsSubis, null, null, null);
    }

    private ResultatTour jouerFuite(Personnage personnage, Combat combat) {
        Chapitre chapitre = recupererChapitre(combat.getChapitreId());
        if (!peutFuir(chapitre, combat)) {
            throw new IllegalArgumentException(
                    "La fuite n'est pas encore possible (seuil d'assauts non atteint pour ce combat)");
        }
        // Reussie a coup sur des qu'elle est proposee ; aucune riposte.
        combat.setStatut(StatutCombat.FUITE);
        return new ResultatTour("FUITE", null, null, null, null, null, null, null, null, null, null);
    }

    // Vrai si au moins un Lien du chapitre porte une condition FUITE dont
    // le seuil d'assauts est deja atteint. Utilise a la fois pour valider
    // l'action FUITE et pour indiquer au frontend si le bouton est actif
    // (voir CombatMapper).
    public boolean peutFuir(Chapitre chapitre, Combat combat) {
        for (Lien lien : chapitre.getLiens()) {
            for (Cond cond : lien.getConditions()) {
                if (cond.getType() == TypeCondition.FUITE && combat.getAssautsLivres() >= parseValeur(cond)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Vrai si une condition ASSAUT_ECHEC du chapitre est atteinte (voir
    // StatutCombat.INTERROMPU).
    private boolean assautEchecAtteint(Chapitre chapitre, Combat combat) {
        for (Lien lien : chapitre.getLiens()) {
            for (Cond cond : lien.getConditions()) {
                if (cond.getType() == TypeCondition.ASSAUT_ECHEC && combat.getAssautsLivres() >= parseValeur(cond)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void passerAuProchainEnnemi(Combat combat) {
        combat.setEnnemiActifIndex(combat.getEnnemiActifIndex() + 1);
        if (combat.getEnnemiActifIndex() >= combat.getEnnemis().size()) {
            combat.setStatut(StatutCombat.VICTOIRE);
        }
    }

    private void appliquerDegatsAuJoueur(Personnage personnage, Combat combat, int degats) {
        if (degats < 0) {
            combat.setEndurancePerdue(true);
        }
        int nouvelleEndurance = Math.max(0, personnage.getEnduranceActuelle() + degats);
        personnage.setEnduranceActuelle(nouvelleEndurance);

        if (nouvelleEndurance <= 0) {
            combat.setStatut(StatutCombat.DEFAITE);
        }
    }

    // HABILETE effective pour un calcul de combat contre CET ennemi : la
    // valeur de base (Personnage.habilite + habiliteTemp) plus le bonus de
    // la Discipline Kai Puissance Psychique (+2), sauf si l'ennemi y
    // resiste (Ennemi.resistances, ex. serpent_aile, gluatre,
    // vordak_puissant). Le bonus est donc recalcule a chaque appel plutot
    // que fixe une fois pour toutes : necessaire pour un combat a
    // plusieurs ennemis ou chacun peut avoir une resistance differente.
    private int habiliteEffective(Personnage personnage, Ennemi ennemi) {
        int base = personnage.getHabilite() + personnage.getHabiliteTemp();
        if (possedePuissancePsychique(personnage) && !resisteAPuissancePsychique(ennemi)) {
            base += 2;
        }
        return base;
    }

    private boolean possedePuissancePsychique(Personnage personnage) {
        return personnage.getDisciplines().stream()
                .anyMatch(discipline -> discipline.getId() == IdDiscipline.PUISSANCE_PSYCHIQUE);
    }

    private boolean resisteAPuissancePsychique(Ennemi ennemi) {
        return ennemi.getResistances().stream()
                .anyMatch(discipline -> discipline.getId() == IdDiscipline.PUISSANCE_PSYCHIQUE);
    }

    private CombatEnnemi ennemiActifRequis(Combat combat) {
        CombatEnnemi actif = combat.getEnnemiActif();
        if (actif == null) {
            throw new IllegalStateException("Aucun ennemi actif : ce combat devrait deja etre termine");
        }
        return actif;
    }

    private Chapitre recupererChapitre(Integer chapitreId) {
        return chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreId));
    }

    // Un personnage mort HORS combat (Personnage.mort, voir
    // EffetChapitreService) ne peut plus initier ni jouer de combat tant
    // qu'il n'a pas ete ressuscite (PersonnageService.ressusciter). Ne
    // concerne pas la mort EN COMBAT (Combat.statut=DEFAITE), geree a part.
    private void verifierPasMort(Personnage personnage) {
        if (personnage.isMort()) {
            throw new IllegalArgumentException(
                    "Ce personnage est mort (perte d'endurance) : ressuscitez-le via "
                            + "POST /personnages/{id}/ressusciter avant de continuer");
        }
    }

    private int parseValeur(Cond cond) {
        try {
            return Integer.parseInt(cond.getValeur().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}