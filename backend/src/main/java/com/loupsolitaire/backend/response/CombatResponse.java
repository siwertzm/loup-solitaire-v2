package com.loupsolitaire.backend.response;

import java.util.List;
import java.util.UUID;

// fuitePossible : vrai si le chapitre propose une condition FUITE dont le
// seuil d'assauts est deja atteint (le frontend peut alors proposer le
// bouton FUITE ; l'appeler avant sinon leve une erreur cote serveur).
public record CombatResponse(
        UUID id,
        Integer chapitreId,
        List<CombatEnnemiResponse> ennemis,
        int assautsLivres,
        boolean endurancePerdue,
        int bonusHabiliteEnAttente,
        String statut,
        boolean fuitePossible) {
}