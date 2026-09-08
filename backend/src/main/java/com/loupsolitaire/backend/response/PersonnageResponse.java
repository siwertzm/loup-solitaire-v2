package com.loupsolitaire.backend.response;

import java.util.List;
import java.util.UUID;

public record PersonnageResponse(
        UUID id,
        String nom,
        int habilite,
        int enduranceMax,
        int enduranceActuelle,
        List<String> disciplines,
        String armeMaitrisee,
        Integer chapitreActuelId,
        List<InventaireItemResponse> inventaire) {
}