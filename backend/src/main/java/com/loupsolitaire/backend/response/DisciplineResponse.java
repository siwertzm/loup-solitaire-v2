package com.loupsolitaire.backend.response;

import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.util.Textes;

public record DisciplineResponse(String id, String nom, String description) {

    public static DisciplineResponse fromEntity(Discipline discipline) {
        return new DisciplineResponse(
                discipline.getId().name(),
                Textes.selon(discipline.getNom(), discipline.getNomEn()),
                Textes.selon(discipline.getDescription(), discipline.getDescriptionEn())
        );
    }
}