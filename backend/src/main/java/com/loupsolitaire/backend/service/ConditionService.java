package com.loupsolitaire.backend.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.CombatRepository;

import lombok.RequiredArgsConstructor;

// Evalue si une Cond (attachee a un Lien) est satisfaite par l'etat actuel
// d'un Personnage.
//
// Evaluables statiquement a partir de l'inventaire/des stats : DISCIPLINE,
// OBJET, ARME, BOURSE, ENDURANCE.
//
// HASARD : evalue via Personnage.dernierTirageHasard, regenere a chaque
// chargement du chapitre (voir PersonnageService.rafraichirTirageHasard) et
// relu (jamais re-tire) ici, pour que la validation au moment du choix
// corresponde exactement a ce que le joueur a vu affiche.
//
// FUITE, ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE, VICTOIRE : evaluees a
// partir du dernier Combat du personnage sur son chapitre ACTUEL (voir
// CombatService). Indisponibles (false) tant qu'aucun combat n'a ete
// resolu sur ce chapitre.
//
// PERMANENT : marqueur, pas une vraie condition d'acces (toujours true).
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final InventaireService inventaireService;
    private final CombatRepository combatRepository;

    // Contexte a partager entre toutes les conditions evaluees pour une meme
    // requete (typiquement tout un chapitre dans ChapitreMapper) : inventaire
    // et combat ne sont alors lus qu'une seule fois (voir ContexteConditions).
    public ContexteConditions nouveauContexte(Personnage personnage) {
        return new ContexteConditions(
                () -> quantitesParObjet(personnage),
                () -> combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(
                        personnage, personnage.getChapitreActuel().getId()));
    }

    // Evaluation isolee : cree son propre contexte (une seule condition,
    // donc au plus une lecture de chaque donnee de toute facon).
    public boolean estDisponible(Cond cond, Personnage personnage) {
        return estDisponible(cond, personnage, nouveauContexte(personnage));
    }

    public boolean estDisponible(Cond cond, Personnage personnage, ContexteConditions contexte) {
        return switch (cond.getType()) {
            case DISCIPLINE -> possedeDiscipline(cond, personnage);
            case OBJET, ARME, BOURSE -> possedeQuantiteObjet(cond, contexte);
            case ENDURANCE -> enduranceSuffisante(cond, personnage);
            case ENDURANCE_INF -> enduranceInferieure(cond, personnage);
            case HASARD -> tirageDansLaPlage(cond, personnage);
            case VICTOIRE, FUITE, ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE -> conditionDeCombat(cond, contexte);
            case PERMANENT -> true;
        };
    }

    // Point d'entree unique pour decider si un Lien (pas juste une Cond
    // isolee) est empruntable, utilise a la fois par ChapitreMapper
    // (affichage, LienResponse.disponible) et PersonnageService
    // (validation avant d'avancer). Un seul et meme calcul pour les deux,
    // pour eviter que l'affichage et la validation divergent.
    public boolean estLienDisponible(Lien lien, Personnage personnage) {
        return estLienDisponible(lien, personnage, nouveauContexte(personnage));
    }

    public boolean estLienDisponible(Lien lien, Personnage personnage, ContexteConditions contexte) {
        return lien.getConditions().stream().allMatch(cond -> estDisponible(cond, personnage, contexte));
    }

    // Cherche le Combat le plus recent du personnage sur son chapitre
    // actuel (peut etre absent si le combat n'a jamais ete engage, ou
    // encore EN_COURS si le joueur n'a pas fini de jouer ses tours).
    private boolean conditionDeCombat(Cond cond, ContexteConditions contexte) {
        Optional<Combat> combatOpt = contexte.combatDuChapitreActuel();
        if (combatOpt.isEmpty()) {
            return false;
        }
        Combat combat = combatOpt.get();

        // La valeur n'est lue que pour les conditions qui en ont besoin :
        // celle de VICTOIRE n'a pas de sens, et celle de FUITE (nombre
        // d'assauts avant de pouvoir fuir) est utilisee par CombatService.
        return switch (cond.getType()) {
            case VICTOIRE -> combat.getStatut() == StatutCombat.VICTOIRE;
            case FUITE -> combat.getStatut() == StatutCombat.FUITE;
            case ASSAUT_MAX -> combat.getStatut() == StatutCombat.VICTOIRE
                    && combat.getAssautsLivres() <= cond.valeurEntiere();
            case ASSAUT_ECHEC -> combat.getStatut() == StatutCombat.INTERROMPU
                    && combat.getAssautsLivres() >= cond.valeurEntiere();
            case ENDURANCE_PERDUE -> combat.getStatut() == StatutCombat.VICTOIRE
                    && (cond.valeurEntiere() == 1) == combat.isEndurancePerdue();
            default -> false;
        };
    }

    private boolean possedeDiscipline(Cond cond, Personnage personnage) {
        IdDiscipline recherchee = IdDiscipline.fromJson(cond.getTargetId());
        return personnage.getDisciplines().stream()
                .anyMatch(d -> d.getId() == recherchee);
    }

    private boolean enduranceInferieure(Cond cond, Personnage personnage) {
        return personnage.getEnduranceActuelle() < cond.valeurEntiere();
    }

    // Meme logique pour OBJET/ARME/BOURSE : possede-t-on au moins la
    // quantite requise de l'objet designe par targetId (ex. "or" pour BOURSE).
    private boolean possedeQuantiteObjet(Cond cond, ContexteConditions contexte) {
        int quantiteRequise = cond.valeurEntiere();
        return contexte.quantitePossedee(cond.getTargetId()) >= quantiteRequise;
    }

    // Quantite totale possedee par id d'objet, en une seule lecture de
    // l'inventaire.
    private Map<String, Integer> quantitesParObjet(Personnage personnage) {
        return inventaireService.listerInventaire(personnage).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getObjet().getId(),
                        Collectors.summingInt(InventaireItem::getQuantite)));
    }

    private boolean enduranceSuffisante(Cond cond, Personnage personnage) {
        return personnage.getEnduranceActuelle() >= cond.valeurEntiere();
    }

    // valeur au format "[min, max]" (ex. "[0, 4]"). Si aucun tirage n'a
    // encore ete fait (personnage jamais passe par GET /chapitre), on
    // considere le lien indisponible plutot que de risquer une validation
    // incorrecte sans tirage reel.
    private boolean tirageDansLaPlage(Cond cond, Personnage personnage) {
        Integer tirage = personnage.getDernierTirageHasard();
        if (tirage == null) {
            return false;
        }

        return cond.plageHasard().contient(tirage);
    }
}