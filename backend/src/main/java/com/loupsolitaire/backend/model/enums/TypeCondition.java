package com.loupsolitaire.backend.model.enums;

public enum TypeCondition {
    DISCIPLINE, OBJET, BOURSE, HASARD, FUITE, ARME, ENDURANCE, ENDURANCE_PERDUE, ASSAUT_MAX, ASSAUT_ECHEC,
    // Marqueur, pas une vraie condition d'acces : indique qu'un Effet
    // modifie une stat de base du joueur de facon definitive plutot que
    // temporairement. Voir EffetService (a venir) pour son traitement special.
    PERMANENT;
 
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
            case "assaut_max" -> ASSAUT_MAX;
            case "assaut_echec" -> ASSAUT_ECHEC;
            case "permanent" -> PERMANENT;
            default -> throw new IllegalArgumentException("Type de condition inconnu : " + valeur);
        };
    }
}
