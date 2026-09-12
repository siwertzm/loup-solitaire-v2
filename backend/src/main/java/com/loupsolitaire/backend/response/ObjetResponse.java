package com.loupsolitaire.backend.response;

import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;

public record ObjetResponse(
        String id,
        String nom,
        String description,
        CategorieObjet categorie
) {
    public static ObjetResponse fromEntity(Objet objet) {
        return new ObjetResponse(
                objet.getId(),
                objet.getNom(),
                objet.getDescription(),
                objet.getCategorie()
        );
    }
}