package com.loupsolitaire.backend.response;

import java.util.List;

// Lecture seule, non filtree : le frontend voit TOUS les liens, conditions
// comprises. Le filtrage selon ce que possede le personnage viendra dans
// une etape suivante.
public record LienResponse(Integer chapitreCibleId, List<CondResponse> conditions) {
}