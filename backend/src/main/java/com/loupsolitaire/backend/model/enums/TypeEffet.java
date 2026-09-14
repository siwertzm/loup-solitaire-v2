package com.loupsolitaire.backend.model.enums;

public enum TypeEffet {
    ENDURANCE, HABILETE, REPAS, VOL, ECHANGE, MORT;

    public static TypeEffet fromJson(String valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("Type d'effet manquant");
        }
        return switch (valeur.trim().toLowerCase()) {
            case "endurance" -> ENDURANCE;
            case "habilite" -> HABILETE;
            case "repas" -> REPAS;
            case "vol" -> VOL;
            case "echange" -> ECHANGE;
            case "mort" -> MORT;
            default -> throw new IllegalArgumentException("Type d'effet inconnu : " + valeur);
        };
    }
}
