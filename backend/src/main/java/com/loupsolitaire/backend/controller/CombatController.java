package com.loupsolitaire.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.request.JouerTourCombatRequest;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.service.PartieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Couche HTTP uniquement : delegue a PartieService (transaction,
// verification du proprietaire, regles du combat).
@RestController
@RequestMapping("/personnages/{id}/combat")
@RequiredArgsConstructor
public class CombatController {

    private final PartieService partieService;

    // Demarre le combat du chapitre courant du personnage, ou renvoie celui
    // deja existant (idempotent). 400 si le chapitre courant n'est pas
    // combat=true.
    @PostMapping
    public ResponseEntity<CombatResponse> initier(
            @PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return ResponseEntity.status(HttpStatus.CREATED).body(partieService.initierCombat(id, connecte));
    }

    // Etat du combat en cours (ou du dernier resolu) sur le chapitre actuel.
    // Ne cree rien : 404 si aucun combat n'a encore ete initie.
    @GetMapping
    public CombatResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {
        return partieService.combatActuel(id, connecte);
    }

    // Joue un tour (ATTAQUE/DEFENSE/OBJET/FUITE). Le serveur fait autorite
    // sur le resultat : le corps de la requete ne contient que le choix
    // d'action, jamais un resultat.
    @PostMapping("/tour")
    public CombatResponse jouerTour(
            @PathVariable UUID id,
            @Valid @RequestBody JouerTourCombatRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        ActionCombat action = versActionCombat(request.getAction());
        return partieService.jouerTour(id, action, request.getObjetId(), connecte);
    }

    private ActionCombat versActionCombat(String valeur) {
        try {
            return ActionCombat.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Action de combat inconnue : " + valeur);
        }
    }
}