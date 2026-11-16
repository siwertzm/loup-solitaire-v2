package com.loupsolitaire.backend.model.enums;

public enum TypeCondition {
    DISCIPLINE, OBJET, BOURSE, HASARD, FUITE, ARME, ENDURANCE, ENDURANCE_PERDUE, ENDURANCE_INF, ASSAUT_MAX, ASSAUT_MIN, ASSAUT_ECHEC, VICTOIRE,
    // Marqueur, pas une vraie condition d'acces : indique qu'un Effet
    // modifie une stat de base du joueur de facon definitive plutot que
    // temporairement. Voir EffetService (a venir) pour son traitement special.
    PERMANENT,
    // REGLE-11 : marqueur d'un effet HABILITE qui remplace l'ajustement
    // d'arme pendant le combat du chapitre (combat a mains nues, 260) :
    // HABILITE de base + valeur, quelles que soient les armes possedees.
    // Applique par CombatService, jamais a l'arrivee sur le chapitre.
    SANS_ARME,
    // D-11 : lien qui permet de ne pas livrer le combat du tout (112, 180).
    // Empruntable avant le premier assaut ; une fois le combat engage, il
    // faut gagner ou fuir (bouton FUITE actif des la premiere action).
    EVITEMENT;
 
    public static TypeCondition fromJson(String valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("Type de condition manquant");
        }
        return switch (valeur.trim().toLowerCase()) {
            case "discipline" -> DISCIPLINE;
            case "objet" -> OBJET;
            case "bourse" -> BOURSE;
            case "hasard" -> HASARD;
            case "fuite" -> FUITE;
            case "arme" -> ARME;
            case "endurance" -> ENDURANCE;
            case "endurance_perdue" -> ENDURANCE_PERDUE;
            case "endurance_inf" -> ENDURANCE_INF;
            case "assaut_max" -> ASSAUT_MAX;
            case "assaut_min" -> ASSAUT_MIN;
            case "assaut_echec" -> ASSAUT_ECHEC;
            case "victoire" -> VICTOIRE;
            case "permanent" -> PERMANENT;
            case "sans_arme" -> SANS_ARME;
            case "evitement" -> EVITEMENT;
            default -> throw new IllegalArgumentException("Type de condition inconnu : " + valeur);
        };
    }
}
