package com.loupsolitaire.backend.model.enums;

public enum CategorieObjet {
    ARME, OBJET, OBJETS_SPECIAUX, REPAS, BOURSE;

    // Convertit la valeur texte libre du JSON source. Leve une exception
    // explicite plutot que d'ignorer silencieusement une categorie inconnue :
    // une erreur de donnees doit se voir au demarrage, pas creer un objet
    // fantome sans categorie.
    public static CategorieObjet fromJson(String valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("Categorie d'objet manquante");
        }
        return switch (valeur.trim().toLowerCase()) {
            case "arme" -> ARME;
            case "objet" -> OBJET;
            case "objets spéciaux", "objets speciaux" -> OBJETS_SPECIAUX;
            case "repas" -> REPAS;
            case "bourse" -> BOURSE;
            default -> throw new IllegalArgumentException("Categorie d'objet inconnue : " + valeur);
        };
    }
}
