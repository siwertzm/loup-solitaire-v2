package com.loupsolitaire.backend.service;

import org.springframework.stereotype.Service;

import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;

import lombok.RequiredArgsConstructor;

// Evalue si une Cond (attachee a un Lien) est satisfaite par l'etat actuel
// d'un Personnage. Seuls 4 types sont evaluables statiquement a partir de
// l'inventaire/des stats : DISCIPLINE, OBJET, BOURSE, ENDURANCE.
//
// Les autres (HASARD, FUITE, ENDURANCE_PERDUE, ASSAUT_MAX, ASSAUT_ECHEC,
// PERMANENT) decrivent une resolution DYNAMIQUE (un jet de de, un etat de
// combat en cours) qui n'existe qu'au moment ou le joueur choisit ce
// chemin : elles ne peuvent jamais bloquer l'affichage d'un lien, toujours
// "disponibles" ici.
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final InventaireService inventaireService;

    public boolean estDisponible(Cond cond, Personnage personnage) {
        return switch (cond.getType()) {
            case DISCIPLINE -> possedeDiscipline(cond, personnage);
            case OBJET, ARME, BOURSE -> possedeQuantiteObjet(cond, personnage);
            case ENDURANCE -> enduranceSuffisante(cond, personnage);
            case HASARD, FUITE, ENDURANCE_PERDUE, ASSAUT_MAX, ASSAUT_ECHEC, PERMANENT -> true;
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

    private int parseValeur(String valeur) {
        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}