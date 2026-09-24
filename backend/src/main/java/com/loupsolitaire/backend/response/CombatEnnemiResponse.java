package com.loupsolitaire.backend.response;

import java.util.List;

public record CombatEnnemiResponse(
        String id,
        String nom,
        int habilite,
        int enduranceMax,
        int enduranceActuelle,
        boolean actif,
        boolean vaincu,
        List<String> resistances,
        List<String> disciplines) {
}