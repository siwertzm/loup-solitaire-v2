package com.loupsolitaire.backend.model.enums;

public enum IdDiscipline {
    CAMOUFLAGE,
    CHASSE,
    SIXIEME_SENS,
    ORIENTATION,
    GUERISON,
    MAITRISE_ARMES,
    BOUCLIER_PSYCHIQUE,
    PUISSANCE_PSYCHIQUE,
    COMMUNICATION_ANIMALE,
    MAITRISE_MATIERE;
 
    public static IdDiscipline fromJson(String valeur) {
        if (valeur == null) {
            throw new IllegalArgumentException("Identifiant de discipline manquant");
        }
        return switch (valeur.trim().toLowerCase()) {
            case "camouflage" -> CAMOUFLAGE;
            case "chasse" -> CHASSE;
            case "sixieme_sens", "sixième_sens" -> SIXIEME_SENS;
            case "orientation" -> ORIENTATION;
            case "guerison", "guérison" -> GUERISON;
            case "maitrise_armes", "maitrise_des_armes" -> MAITRISE_ARMES;
            case "bouclier_psychique" -> BOUCLIER_PSYCHIQUE;
            case "puissance_psychique" -> PUISSANCE_PSYCHIQUE;
            case "communication_animale" -> COMMUNICATION_ANIMALE;
            case "maitrise_matiere" -> MAITRISE_MATIERE;
            default -> throw new IllegalArgumentException("Discipline inconnue : " + valeur);
        };
    }
}
