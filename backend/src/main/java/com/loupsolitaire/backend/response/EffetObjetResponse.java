package com.loupsolitaire.backend.response;

// Effet d'un OBJET consommable (endurance/habilite), distinct de EffetResponse
// (effets de CHAPITRE, qui portent des conditions) : un effet d'objet
// s'applique toujours sans condition au moment de la consommation
// (voir ObjetService.appliquerEffetsConsommation).
public record EffetObjetResponse(String type, Integer valeur) {
}