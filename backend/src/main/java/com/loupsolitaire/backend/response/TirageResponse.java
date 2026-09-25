package com.loupsolitaire.backend.response;

// Tirage de creation (POST /personnages/tirage) : le chiffre tire (0-9) et
// la caracteristique qui en decoule, pour que l'ecran affiche le de et le
// total sans refaire le calcul.
public record TirageResponse(
        int hasardHabilite,
        int hasardEndurance,
        int habilite,
        int endurance
) {
}