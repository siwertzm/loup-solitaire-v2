package com.loupsolitaire.backend.service.record;

// Detail transitoire d'UN tour de combat, calcule par CombatService et non
// persiste (Combat ne garde que l'etat cumule : endurances, assauts...).
// Sert uniquement a l'affichage cote client (CombatController/CombatMapper) :
// le rapport de combat et les tirages exacts qui ont produit les degats.
//
// Les champs non pertinents pour l'action jouee restent a null plutot que 0,
// pour que le client distingue "pas de riposte ce tour" (FUITE) de
// "riposte qui n'a fait aucun degat" (0).
//
// rapportAttaque/tirageAttaque/degatsInfliges : uniquement ATTAQUE.
// rapportRiposte/tirageRiposte/degatsSubisBruts/degatsSubis : ATTAQUE/DEFENSE/OBJET
//   (jamais FUITE, qui n'a pas de riposte).
// reductionPourcent/bonusHabiliteObtenu : uniquement DEFENSE.
public record ResultatTour(
        String action,
        Integer rapportAttaque,
        Integer tirageAttaque,
        Integer degatsInfliges,
        Integer rapportRiposte,
        Integer tirageRiposte,
        Integer degatsSubisBruts,
        Integer degatsSubis,
        Integer tirageDefense,
        Integer reductionPourcent,
        Integer bonusHabiliteObtenu) {
}