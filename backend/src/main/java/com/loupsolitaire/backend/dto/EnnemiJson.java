package com.loupsolitaire.backend.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnnemiJson {
    private String id;
    private String nom;
    private String description;
    private int habilite;
    private int endurance;
    private List<RefJson> resistance;
    // Disciplines que l'ennemi utilise contre le joueur (ex. puissance_psychique
    // pour les Vordaks). Optionnel, absent = aucune.
    private List<RefJson> disciplines;
}