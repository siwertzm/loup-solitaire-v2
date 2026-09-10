package com.loupsolitaire.backend.response;

import java.util.List;
import java.util.UUID;

// volEnAttente : non-null si un Effet VOL (valeur=1) attend que le joueur
// choisisse quoi perdre ("ARME" ou "TOUT", voir PorteeVol). Le frontend
// doit alors appeler POST /vol/{objetId} avant de pouvoir continuer.
//
// mort : true si l'ENDURANCE est tombee a 0 HORS combat (effet ENDURANCE
// ou REPAS d'un chapitre). Bloque toute action jusqu'a POST /ressusciter.
// Ne concerne PAS une defaite en combat (voir Combat.statut=DEFAITE et
// POST /chapitre/revenir-apres-defaite a la place).
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
        boolean mort,
        List<InventaireItemResponse> inventaire) {
}