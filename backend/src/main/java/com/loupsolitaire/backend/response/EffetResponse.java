package com.loupsolitaire.backend.response;

import java.util.List;

public record EffetResponse(
        String type,
        Integer valeur,
        String nom,
        List<CondResponse> conditions,
        String resultat) {
}