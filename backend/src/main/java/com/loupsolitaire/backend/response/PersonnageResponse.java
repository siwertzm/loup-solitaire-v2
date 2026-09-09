package com.loupsolitaire.backend.response;

import java.util.List;
import java.util.UUID;

// volEnAttente : non-null si un Effet VOL (valeur=1) attend que le joueur
// choisisse quoi perdre ("ARME" ou "TOUT", voir PorteeVol). Le frontend
// doit alors appeler POST /vol/{objetId} avant de pouvoir continuer.
public record PersonnageResponse(
        UUID id,
        String nom,
        int habiliteBase,
        int habilite,
        int habiliteTemp,
        int enduranceMax,
        int enduranceActuelle,
        List<String> disciplines,
        String armeMaitrisee,
        Integer chapitreActuelId,
        String volEnAttente,
        List<InventaireItemResponse> inventaire) {
}