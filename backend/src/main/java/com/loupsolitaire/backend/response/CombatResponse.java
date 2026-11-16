package com.loupsolitaire.backend.response;

import java.util.List;
import java.util.UUID;

// fuitePossible : vrai si le chapitre propose une condition FUITE dont le
// seuil d'assauts est deja atteint (le frontend peut alors proposer le
// bouton FUITE ; l'appeler avant sinon leve une erreur cote serveur).
//
// dernierTour : detail du calcul du tour qui vient d'etre joue (rapport de
// combat, tirages, degats, % de reduction). Null sur GET/POST /combat,
// rempli uniquement par POST /combat/tour.
public record CombatResponse(
        UUID id,
        Integer chapitreId,
        List<CombatEnnemiResponse> ennemis,
        int assautsLivres,
        boolean endurancePerdue,
        int bonusHabiliteEnAttente,
        String statut,
        boolean fuitePossible,
        // REGLE-11 : malus d'HABILETE du combat a mains nues (-4 au 260),
        // qui remplace l'ajustement d'arme ; null pour les autres combats.
        Integer malusMainsNues,
        // REGLE-10 : bonus/malus d'HABILETE de l'assaut a venir, deja compte
        // dans les calculs du serveur (283 : +2 au 1er, -2 ensuite sans
        // Bouclier Psychique) ; 0 sinon.
        int bonusHabiliteAssaut,
        ResultatTourResponse dernierTour) {
}