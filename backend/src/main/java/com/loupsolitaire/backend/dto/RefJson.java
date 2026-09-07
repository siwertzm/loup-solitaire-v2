package com.loupsolitaire.backend.dto;

import lombok.Getter;
import lombok.Setter;

// Forme minimale {"id": "..."} utilisee partout dans le JSON source pour
// referencer une entite du catalogue (discipline, ennemi) par son id, sans
// dupliquer ses donnees. Reutilise pour Ennemi.resistance et plus tard pour
// Chapitre.ennemi.
@Getter
@Setter
public class RefJson {
    private String id;
}