package com.loupsolitaire.backend.response;

import java.util.List;

// "disponible" : calcule a partir de l'etat du personnage (voir
// ConditionService). Les liens indisponibles sont quand meme renvoyes
// (griser cote frontend), jamais caches.
public record LienResponse(Integer chapitreCibleId, boolean disponible, List<CondResponse> conditions) {
}