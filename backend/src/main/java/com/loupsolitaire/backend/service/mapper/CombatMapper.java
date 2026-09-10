package com.loupsolitaire.backend.service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.CombatEnnemiResponse;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.response.ResultatTourResponse;
import com.loupsolitaire.backend.service.CombatService;
import com.loupsolitaire.backend.service.record.ResultatTour;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CombatMapper {

    private final ChapitreRepository chapitreRepository;
    private final CombatService combatService;

    // Utilise par GET/POST /combat : aucun tour ne vient d'etre joue.
    @Transactional(readOnly = true)
    public CombatResponse versReponse(Combat combat) {
        return versReponse(combat, null);
    }

    // Utilise par POST /combat/tour : inclut le detail du calcul (rapport
    // de combat, tirages, degats, % de reduction) dans "dernierTour".
    @Transactional(readOnly = true)
    public CombatResponse versReponse(Combat combat, ResultatTour resultat) {
        List<CombatEnnemiResponse> ennemis = combat.getEnnemis().stream()
                .map(ce -> versReponseEnnemi(ce, combat))
                .toList();

        boolean fuitePossible = combat.getStatut() == StatutCombat.EN_COURS && peutFuir(combat);

        return new CombatResponse(
                combat.getId(),
                combat.getChapitreId(),
                ennemis,
                combat.getAssautsLivres(),
                combat.isEndurancePerdue(),
                combat.getBonusHabiliteEnAttente(),
                combat.getStatut().name(),
                fuitePossible,
                versReponseResultat(resultat));
    }

    private ResultatTourResponse versReponseResultat(ResultatTour resultat) {
        if (resultat == null) {
            return null;
        }
        return new ResultatTourResponse(
                resultat.action(),
                resultat.rapportAttaque(),
                resultat.tirageAttaque(),
                resultat.degatsInfliges(),
                resultat.rapportRiposte(),
                resultat.tirageRiposte(),
                resultat.degatsSubisBruts(),
                resultat.degatsSubis(),
                resultat.tirageDefense(),
                resultat.reductionPourcent(),
                resultat.bonusHabiliteObtenu());
    }

    private boolean peutFuir(Combat combat) {
        Chapitre chapitre = chapitreRepository.findById(combat.getChapitreId())
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + combat.getChapitreId()));
        return combatService.peutFuir(chapitre, combat);
    }

    private CombatEnnemiResponse versReponseEnnemi(CombatEnnemi ce, Combat combat) {
        boolean vaincu = ce.getEnduranceActuelle() <= 0;
        boolean actif = !vaincu && combat.getEnnemiActif() == ce;
        return new CombatEnnemiResponse(
                ce.getEnnemi().getId(),
                ce.getEnnemi().getNom(),
                ce.getEnnemi().getHabilite(),
                ce.getEnnemi().getEndurance(),
                ce.getEnduranceActuelle(),
                actif,
                vaincu);
    }
}