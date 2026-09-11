package com.loupsolitaire.backend.response;

import com.loupsolitaire.backend.model.Discipline;

public record DisciplineResponse(String id, String nom, String description) {

    public static DisciplineResponse fromEntity(Discipline discipline) {
        return new DisciplineResponse(
                discipline.getId().name(),
                discipline.getNom(),
                discipline.getDescription()
        );
    }
}