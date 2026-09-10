package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JouerTourCombatRequest {

    // Nom exact d'une valeur de l'enum ActionCombat : "ATTAQUE", "DEFENSE",
    // "OBJET" ou "FUITE".
    @NotBlank(message = "L'action est obligatoire")
    private String action;

    // Requis uniquement quand action="OBJET" : l'id de l'objet consommable
    // a utiliser (doit etre possede par le personnage, categorie OBJET).
    private String objetId;
}