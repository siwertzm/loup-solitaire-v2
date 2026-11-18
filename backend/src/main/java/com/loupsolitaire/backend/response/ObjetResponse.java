package com.loupsolitaire.backend.response;

import java.util.List;

import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.util.Textes;

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
                Textes.selon(objet.getNom(), objet.getNomEn()),
                Textes.selon(objet.getDescription(), objet.getDescriptionEn()),
                objet.getCategorie(),
                objet.getEffets().stream()
                        .map(e -> new EffetObjetResponse(e.getType().name(), e.getValeur()))
                        .toList()
        );
    }
}