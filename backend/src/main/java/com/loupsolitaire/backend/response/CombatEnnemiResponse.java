package com.loupsolitaire.backend.response;

public record CombatEnnemiResponse(
        String id,
        String nom,
        int habilite,
        int enduranceMax,
        int enduranceActuelle,
        boolean actif,
        boolean vaincu) {
}