package com.loupsolitaire.backend.model.enums;

// Les 4 choix du joueur a son tour (voir CombatService.jouerTour) :
// ATTAQUE  : degats a l'ennemi via TABLE_LS, puis riposte de l'ennemi via TABLE_E.
// DEFENSE  : aucun degat inflige ; le tirage de reduction/bonus s'applique
//            immediatement a la riposte encaissee CE tour, et le bonus
//            d'HABILITE reste disponible pour la PROCHAINE attaque.
// OBJET    : consomme un objet (categorie OBJET, ex. Potion/Laumspur), meme
//            mecanique que POST /objets/{objetId}/consommer ; l'ennemi
//            riposte quand meme, sans attenuation.
// FUITE    : n'est propose que si le Lien de sortie correspondant a une
//            condition FUITE dont le seuil d'assauts est atteint. L'ennemi
//            porte un dernier coup (REGLE-03, comme dans le livre) ; la
//            fuite reussit sauf si ce coup tue Loup Solitaire.
public enum ActionCombat {
    ATTAQUE,
    DEFENSE,
    OBJET,
    FUITE
}