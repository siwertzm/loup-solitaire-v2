package com.loupsolitaire.backend.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.request.JouerTourCombatRequest;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.service.CombatService;
import com.loupsolitaire.backend.service.mapper.CombatMapper;
import com.loupsolitaire.backend.service.record.TourJoue;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/personnages/{id}/combat")
@RequiredArgsConstructor
public class CombatController {

    private final CombatService combatService;
    private final CombatMapper combatMapper;
    private final PersonnageRepository personnageRepository;
    private final ObjetRepository objetRepository;

    // Demarre le combat du chapitre courant du personnage, ou renvoie celui
    // deja EN_COURS (idempotent : rouvrir l'ecran ne relance pas le combat
    // a zero). 400 si le chapitre courant n'est pas combat=true.
    @PostMapping
    @Transactional
    public ResponseEntity<CombatResponse> initier(
            @PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {

        Personnage personnage = recupererPourModification(id, connecte);
        Combat combat = combatService.initierCombat(personnage);
        return ResponseEntity.status(HttpStatus.CREATED).body(combatMapper.versReponse(combat));
    }

    // Etat courant du combat en cours (ou du dernier resolu) sur le
    // chapitre actuel du personnage. Contrairement a POST, ne cree rien :
    // 404 si aucun combat n'a encore ete initie sur ce chapitre.
    @GetMapping
    @Transactional
    public CombatResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {
        Personnage personnage = recupererEtVerifierProprietaire(id, connecte);
        Combat combat = combatService.combatActuel(personnage)
                .orElseThrow(() -> new RessourceNonTrouveeException(
                        "Aucun combat en cours sur le chapitre " + personnage.getChapitreActuel().getId()));
        return combatMapper.versReponse(combat);
    }

    // Joue un tour (ATTAQUE/DEFENSE/OBJET/FUITE). Le serveur fait autorite
    // sur le resultat (tirages, table de resolution) : le corps de la
    // requete ne contient que le choix d'action, jamais un resultat.
    @PostMapping("/tour")
    @Transactional
    public CombatResponse jouerTour(
            @PathVariable UUID id,
            @Valid @RequestBody JouerTourCombatRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        Personnage personnage = recupererPourModification(id, connecte);

        ActionCombat action = versActionCombat(request.getAction());
        Objet objet = request.getObjetId() != null ? recupererObjet(request.getObjetId()) : null;

        TourJoue tourJoue = combatService.jouerTour(personnage, action, objet);
        return combatMapper.versReponse(tourJoue.combat(), tourJoue.resultat());
    }

    private ActionCombat versActionCombat(String valeur) {
        try {
            return ActionCombat.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Action de combat inconnue : " + valeur);
        }
    }

    private Objet recupererObjet(String objetId) {
        return objetRepository.findById(objetId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Objet introuvable : " + objetId));
    }

    // Lecture seule (GET).
    private Personnage recupererEtVerifierProprietaire(UUID id, UtilisateurConnecte connecte) {
        return verifierProprietaire(id, personnageRepository.findById(id), connecte);
    }

    // Toute action qui modifie le personnage : protege contre deux requetes
    // simultanees (voir PersonnageRepository.findByIdPourModification).
    private Personnage recupererPourModification(UUID id, UtilisateurConnecte connecte) {
        return verifierProprietaire(id, personnageRepository.findByIdPourModification(id), connecte);
    }

    private Personnage verifierProprietaire(UUID id, Optional<Personnage> trouve, UtilisateurConnecte connecte) {
        Personnage personnage = trouve
                .orElseThrow(() -> new RessourceNonTrouveeException("Personnage introuvable : " + id));

        // Comparaison d'identifiants : l'utilisateur du personnage est deja
        // charge avec lui, aucune requete supplementaire.
        if (!personnage.getUtilisateur().getId().equals(connecte.id())) {
            throw new AccesRefuseException("Ce personnage ne vous appartient pas");
        }

        return personnage;
    }
}