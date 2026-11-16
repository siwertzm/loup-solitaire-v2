package com.loupsolitaire.backend.model.enums;

// Portee d'un vol (Effet VOL, valeur=1) en attente de resolution par le
// joueur : ARME = une arme au choix (277) ; TOUT = un objet ou repas du Sac,
// ou une arme seulement si le Sac n'en contient aucun (144, REGLE-09).
public enum PorteeVol {
    ARME,
    TOUT
}