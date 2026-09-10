package com.loupsolitaire.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.InventaireItem;
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
// FUITE, ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE : evaluees a partir du
// dernier Combat du personnage sur son chapitre ACTUEL (voir CombatService).
// Indisponibles (false) tant qu'aucun combat n'a ete resolu sur ce chapitre.
//
// PERMANENT : marqueur, pas une vraie condition d'acces (toujours true).
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final InventaireService inventaireService;
    private final CombatRepository combatRepository;

    public boolean estDisponible(Cond cond, Personnage personnage) {
        return switch (cond.getType()) {
            case DISCIPLINE -> possedeDiscipline(cond, personnage);
            case OBJET, ARME, BOURSE -> possedeQuantiteObjet(cond, personnage);
            case ENDURANCE -> enduranceSuffisante(cond, personnage);
            case HASARD -> tirageDansLaPlage(cond, personnage);
            case FUITE, ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE -> conditionDeCombat(cond, personnage);
            case PERMANENT -> true;
        };
    }

    // Cherche le Combat le plus recent du personnage sur son chapitre
    // actuel (peut etre absent si le combat n'a jamais ete engage, ou
    // encore EN_COURS si le joueur n'a pas fini de jouer ses tours).
    private boolean conditionDeCombat(Cond cond, Personnage personnage) {
        Optional<Combat> combatOpt = combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(
                personnage, personnage.getChapitreActuel().getId());
        if (combatOpt.isEmpty()) {
            return false;
        }
        Combat combat = combatOpt.get();
        int valeur = parseValeur(cond.getValeur());

        return switch (cond.getType()) {
            case FUITE -> combat.getStatut() == StatutCombat.FUITE;
            case ASSAUT_MAX -> combat.getStatut() == StatutCombat.VICTOIRE && combat.getAssautsLivres() <= valeur;
            case ASSAUT_ECHEC -> combat.getStatut() == StatutCombat.INTERROMPU && combat.getAssautsLivres() >= valeur;
            case ENDURANCE_PERDUE -> combat.getStatut() == StatutCombat.VICTOIRE
                    && (valeur == 1) == combat.isEndurancePerdue();
            default -> false;
        };
    }

    private boolean possedeDiscipline(Cond cond, Personnage personnage) {
        IdDiscipline recherchee = IdDiscipline.fromJson(cond.getTargetId());
        return personnage.getDisciplines().stream()
                .anyMatch(d -> d.getId() == recherchee);
    }

    // Meme logique pour OBJET/ARME/BOURSE : possede-t-on au moins la
    // quantite requise de l'objet designe par targetId (ex. "or" pour BOURSE).
    private boolean possedeQuantiteObjet(Cond cond, Personnage personnage) {
        int quantiteRequise = parseValeur(cond.getValeur());
        int quantitePossedee = inventaireService.listerInventaire(personnage).stream()
                .filter(item -> item.getObjet().getId().equals(cond.getTargetId()))
                .mapToInt(InventaireItem::getQuantite)
                .sum();
        return quantitePossedee >= quantiteRequise;
    }

    private boolean enduranceSuffisante(Cond cond, Personnage personnage) {
        return personnage.getEnduranceActuelle() >= parseValeur(cond.getValeur());
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

        String valeur = cond.getValeur().replace("[", "").replace("]", "").trim();
        String[] bornes = valeur.split(",");
        int min = Integer.parseInt(bornes[0].trim());
        int max = Integer.parseInt(bornes[1].trim());

        return tirage >= min && tirage <= max;
    }

    private int parseValeur(String valeur) {
        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}