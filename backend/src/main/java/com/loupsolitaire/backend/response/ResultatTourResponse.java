package com.loupsolitaire.backend.response;

// Detail du tour qui vient d'etre joue (POST /combat/tour uniquement ;
// null sur GET/POST /combat, ou aucun tour n'a ete joue).
//
// Champs a null quand non pertinents pour l'action jouee :
// - rapportAttaque/tirageAttaque/degatsInfliges : uniquement ATTAQUE.
// - rapportRiposte/tirageRiposte/degatsSubisBruts/degatsSubis : jamais pour FUITE.
// - reductionPourcent/bonusHabiliteObtenu : uniquement DEFENSE.
public record ResultatTourResponse(
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