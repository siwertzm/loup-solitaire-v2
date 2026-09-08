package com.loupsolitaire.backend.service;

import org.springframework.stereotype.Service;

import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;

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
// FUITE, ENDURANCE_PERDUE, ASSAUT_MAX, ASSAUT_ECHEC, PERMANENT : decrivent
// un etat de COMBAT en cours, qui n'existe pas encore dans le systeme.
// Toujours "disponibles" ici ; a traiter quand le combat sera construit.
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final InventaireService inventaireService;

    public boolean estDisponible(Cond cond, Personnage personnage) {
        return switch (cond.getType()) {
            case DISCIPLINE -> possedeDiscipline(cond, personnage);
            case OBJET, ARME, BOURSE -> possedeQuantiteObjet(cond, personnage);
            case ENDURANCE -> enduranceSuffisante(cond, personnage);
            case HASARD -> tirageDansLaPlage(cond, personnage);
            case FUITE, ENDURANCE_PERDUE, ASSAUT_MAX, ASSAUT_ECHEC, PERMANENT -> true;
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