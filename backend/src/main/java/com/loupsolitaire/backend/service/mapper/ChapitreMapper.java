package com.loupsolitaire.backend.service.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.ObjetChap;
import com.loupsolitaire.backend.model.ObjetChapitreRamasse;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.ObjetChapitreRamasseRepository;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.CondResponse;
import com.loupsolitaire.backend.response.EffetResponse;
import com.loupsolitaire.backend.response.EnnemiChapitreResponse;
import com.loupsolitaire.backend.response.LienResponse;
import com.loupsolitaire.backend.response.ObjetChapResponse;
import com.loupsolitaire.backend.service.ConditionService;

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
    private final ConditionService conditionService;
    private final ObjetChapitreRamasseRepository objetChapitreRamasseRepository;

    // "personnage" sert a calculer LienResponse.disponible (voir
    // ConditionService) ET la quantite RESTANTE des objets optionnels (voir
    // versReponseObjet) : le reste du chapitre est identique pour tout le monde.
    @Transactional(readOnly = true)
    public ChapitreResponse versReponse(Integer chapitreId, Personnage personnage) {
        Chapitre chapitre = chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreId));

        List<EnnemiChapitreResponse> ennemis = chapitre.getEnnemis().stream()
                .map(e -> new EnnemiChapitreResponse(e.getId(), e.getNom(), e.getHabilite(), e.getEndurance()))
                .toList();

        List<EffetResponse> effets = chapitre.getEffets().stream()
                .map(this::versReponseEffet)
                .toList();

        List<LienResponse> liens = chapitre.getLiens().stream()
                .map(lien -> versReponseLien(lien, personnage))
                .toList();

        // Deja ramasse ici, par objetId : une seule requete pour tout le
        // chapitre plutot qu'une par objet (voir Repository).
        Map<String, Integer> dejaPrisParObjet = objetChapitreRamasseRepository
                .findByPersonnageIdAndChapitreId(personnage.getId(), chapitreId).stream()
                .collect(Collectors.toMap(r -> r.getObjet().getId(), ObjetChapitreRamasse::getQuantite));

        List<ObjetChapResponse> objets = chapitre.getObjets().stream()
                .map(o -> versReponseObjet(o, dejaPrisParObjet))
                .toList();

        return new ChapitreResponse(chapitre.getId(), chapitre.getText(), chapitre.isCombat(),
                personnage.getDernierTirageHasard(), ennemis, effets, liens, objets);
    }

    private ObjetChapResponse versReponseObjet(ObjetChap objetChap, Map<String, Integer> dejaPrisParObjet) {

        String objetId = objetChap.getObjet().getId();
        int valeur = objetChap.getValeur();

        // Uniquement pour les valeurs POSITIVES (un ajout, pas un paiement/
        // vol a valeur negative, qui n'a pas de notion de "restant a
        // prendre") : optionnel ET obligatoire sont maintenant traces dans
        // ObjetChapitreRamasse (voir PersonnageService.appliquerObjetChap
        // pour les obligatoires — la categorie pouvait etre pleine a
        // l'arrivee — et ramasserObjetDuChapitre pour les deux cas).
        if (valeur > 0) {
            int dejaPris = dejaPrisParObjet.getOrDefault(objetId, 0);
            valeur = Math.max(0, objetChap.getValeur() - dejaPris);
        }

        return new ObjetChapResponse(objetId, objetChap.getObjet().getNom(), valeur, objetChap.isOptionnel());
    }

    private EffetResponse versReponseEffet(Effet effet) {
        List<CondResponse> conditions = effet.getConditions().stream()
                .map(this::versReponseCond)
                .toList();
        return new EffetResponse(effet.getType().name(), effet.getValeur(), conditions);
    }

    private LienResponse versReponseLien(Lien lien, Personnage personnage) {
        List<CondResponse> conditions = lien.getConditions().stream()
                .map(this::versReponseCond)
                .toList();

        boolean disponible = conditionService.estLienDisponible(lien, personnage);

        return new LienResponse(lien.getChapitreCible().getId(), disponible, conditions);
    }

    private CondResponse versReponseCond(Cond cond) {
        return new CondResponse(cond.getType().name(), cond.getTargetId(), cond.getValeur());
    }
}