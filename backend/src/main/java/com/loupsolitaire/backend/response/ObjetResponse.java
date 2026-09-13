package com.loupsolitaire.backend.response;

import java.util.List;

import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;

public record ObjetResponse(
        String id,
        String nom,
        String description,
        CategorieObjet categorie,
        List<EffetObjetResponse> effets
) {
    public static ObjetResponse fromEntity(Objet objet) {
        return new ObjetResponse(
                objet.getId(),
                objet.getNom(),
                objet.getDescription(),
                objet.getCategorie(),
                objet.getEffets().stream()
                        .map(e -> new EffetObjetResponse(e.getType().name(), e.getValeur()))
                        .toList()
        );
    }
}