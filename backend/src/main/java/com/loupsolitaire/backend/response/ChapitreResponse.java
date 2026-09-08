package com.loupsolitaire.backend.response;

import java.util.List;

public record ChapitreResponse(
        Integer id,
        String text,
        boolean combat,
        List<EnnemiChapitreResponse> ennemis,
        List<EffetResponse> effets,
        List<LienResponse> liens,
        List<ObjetChapResponse> objets) {
}