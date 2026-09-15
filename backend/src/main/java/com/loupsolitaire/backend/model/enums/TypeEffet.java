package com.loupsolitaire.backend.model.enums;

public enum TypeEffet {
    ENDURANCE, HABILITE, REPAS, VOL, ECHANGE, MORT;

    public static TypeEffet fromJson(String valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("Type d'effet manquant");
        }
        return switch (valeur.trim().toLowerCase()) {
            case "endurance" -> ENDURANCE;
            case "habilite" -> HABILITE;
            case "repas" -> REPAS;
            case "vol" -> VOL;
            case "echange" -> ECHANGE;
            case "mort" -> MORT;
            default -> throw new IllegalArgumentException("Type d'effet inconnu : " + valeur);
        };
    }
}
