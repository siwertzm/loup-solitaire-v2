package com.loupsolitaire.backend.model.enums;

// Cycle de vie d'un Combat : EN_COURS tant qu'aucun denouement n'est
// atteint. Une fois VICTOIRE/DEFAITE/FUITE/INTERROMPU, le combat est
// termine et ne peut plus recevoir de tour (voir CombatService.jouerTour).
public enum StatutCombat {
    EN_COURS,
    VICTOIRE,
    DEFAITE,
    FUITE,

    // Le seuil d'assauts d'une condition ASSAUT_ECHEC du chapitre est
    // atteint sans que l'ennemi actif soit mort (ex. chapitre 231 : "si
    // apres 4 assauts il est toujours vivant, rendez-vous au 203"). Fixe
    // automatiquement par CombatService a la fin d'un tour, jamais choisi
    // par le joueur.
    INTERROMPU
}