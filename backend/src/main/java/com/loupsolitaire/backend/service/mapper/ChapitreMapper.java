package com.loupsolitaire.backend.service.mapper;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.CondResponse;
import com.loupsolitaire.backend.response.EffetResponse;
import com.loupsolitaire.backend.response.EnnemiChapitreResponse;
import com.loupsolitaire.backend.response.LienResponse;
import com.loupsolitaire.backend.response.ObjetChapResponse;

import lombok.RequiredArgsConstructor;

// Chapitre a QUATRE collections (ennemis, effets, liens, objets) : les
// charger toutes en une seule requete via @EntityGraph provoquerait une
// MultipleBagFetchException (Hibernate ne peut pas JOIN FETCH plusieurs
// collections "bag" a la fois). Solution : une methode @Transactional qui
// recharge le Chapitre puis touche chaque collection separement, en
// restant dans la meme session (N+1 requetes, mais sans risque et
// largement suffisant pour un seul chapitre a la fois).
@Service
@RequiredArgsConstructor
public class ChapitreMapper {

    private final ChapitreRepository chapitreRepository;

    @Transactional(readOnly = true)
    public ChapitreResponse versReponse(Integer chapitreId) {
        Chapitre chapitre = chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreId));

        List<EnnemiChapitreResponse> ennemis = chapitre.getEnnemis().stream()
                .map(e -> new EnnemiChapitreResponse(e.getId(), e.getNom(), e.getHabilite(), e.getEndurance()))
                .toList();

        List<EffetResponse> effets = chapitre.getEffets().stream()
                .map(this::versReponseEffet)
                .toList();

        List<LienResponse> liens = chapitre.getLiens().stream()
                .map(this::versReponseLien)
                .toList();

        List<ObjetChapResponse> objets = chapitre.getObjets().stream()
                .map(o -> new ObjetChapResponse(o.getObjet().getId(), o.getObjet().getNom(), o.getValeur(), o.isOptionnel()))
                .toList();

        return new ChapitreResponse(chapitre.getId(), chapitre.getText(), chapitre.isCombat(),
                ennemis, effets, liens, objets);
    }

    private EffetResponse versReponseEffet(Effet effet) {
        List<CondResponse> conditions = effet.getConditions().stream()
                .map(this::versReponseCond)
                .toList();
        return new EffetResponse(effet.getType().name(), effet.getValeur(), conditions);
    }

    private LienResponse versReponseLien(Lien lien) {
        List<CondResponse> conditions = lien.getConditions().stream()
                .map(this::versReponseCond)
                .toList();
        return new LienResponse(lien.getChapitreCible().getId(), conditions);
    }

    private CondResponse versReponseCond(Cond cond) {
        return new CondResponse(cond.getType().name(), cond.getTargetId(), cond.getValeur());
    }
}
