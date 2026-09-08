package com.loupsolitaire.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.CreerPersonnageRequest;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.mapper.ChapitreMapper;
import com.loupsolitaire.backend.service.InventaireService;
import com.loupsolitaire.backend.service.ObjetService;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;
import com.loupsolitaire.backend.service.PersonnageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/personnages")
@RequiredArgsConstructor
public class PersonnageController {

    private final PersonnageService personnageService;
    private final PersonnageRepository personnageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjetRepository objetRepository;
    private final InventaireService inventaireService;
    private final ObjetService objetService;
    private final PersonnageMapper personnageMapper;
    private final ChapitreMapper chapitreMapper;

    @PostMapping
    public ResponseEntity<PersonnageResponse> creer(
            @Valid @RequestBody CreerPersonnageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Utilisateur utilisateur = recupererUtilisateur(userDetails);

        List<IdDiscipline> disciplines = request.getDisciplines().stream()
                .map(this::versIdDiscipline)
                .toList();

        Personnage personnage = personnageService.creerPersonnage(utilisateur, request.getNom(), disciplines);

        return ResponseEntity.status(HttpStatus.CREATED).body(personnageMapper.versReponse(personnage));
    }

    // Ecran profil : la liste des personnages de l'utilisateur connecte.
    @GetMapping
    public List<PersonnageResponse> lister(@AuthenticationPrincipal UserDetails userDetails) {
        Utilisateur utilisateur = recupererUtilisateur(userDetails);

        return personnageRepository.findByUtilisateur(utilisateur).stream()
                .map(personnageMapper::versReponse)
                .toList();
    }

    // Fiche personnage complete (reprise d'une partie).
    @GetMapping("/{id}")
    public PersonnageResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        return personnageMapper.versReponse(personnage);
    }

    // Chapitre courant du personnage, en lecture seule : liens et
    // conditions non filtres pour l'instant (etape suivante).
    @GetMapping("/{id}/chapitre")
    public ChapitreResponse chapitreCourant(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        return chapitreMapper.versReponse(personnage.getChapitreActuel().getId());
    }

    // Endpoint de test/debug : ajoute un objet du catalogue a l'inventaire
    // du personnage (limites, plafonnement, bonus d'armure a la
    // recuperation - les consommables n'ont plus d'effet ici).
    @PostMapping("/{id}/objets/{objetId}")
    public PersonnageResponse ajouterObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @RequestParam(defaultValue = "1") int quantite,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objet = recupererObjet(objetId);

        inventaireService.ajouterObjet(personnage, objet, quantite);

        return personnageMapper.versReponse(personnage);
    }

    // Endpoint de test/debug symetrique : retire un objet possede, sans
    // jamais appliquer d'effet de consommable (voir /consommer ci-dessous).
    @DeleteMapping("/{id}/objets/{objetId}")
    public PersonnageResponse retirerObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @RequestParam(defaultValue = "1") int quantite,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objet = recupererObjet(objetId);

        inventaireService.retirerObjet(personnage, objet, quantite);

        return personnageMapper.versReponse(personnage);
    }

    // Consomme 1 exemplaire d'un objet (potion, laumspur, essence
    // d'Alether...) : applique son effet PUIS le retire de l'inventaire.
    // Contrairement a DELETE /objets/{objetId}, celui-ci modifie les stats.
    @PostMapping("/{id}/objets/{objetId}/consommer")
    public PersonnageResponse consommerObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objet = recupererObjet(objetId);

        objetService.appliquerEffetsConsommation(personnage, objet);
        inventaireService.retirerObjet(personnage, objet, 1);

        return personnageMapper.versReponse(personnage);
    }

    private Personnage recupererEtVerifierProprietaire(UUID id, UserDetails userDetails) {
        Personnage personnage = personnageRepository.findById(id)
                .orElseThrow(() -> new RessourceNonTrouveeException("Personnage introuvable : " + id));

        if (!personnage.getUtilisateur().getUsername().equals(userDetails.getUsername())) {
            throw new AccesRefuseException("Ce personnage ne vous appartient pas");
        }

        return personnage;
    }

    private Objet recupererObjet(String objetId) {
        return objetRepository.findById(objetId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Objet introuvable : " + objetId));
    }

    private Utilisateur recupererUtilisateur(UserDetails userDetails) {
        return utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));
    }

    private IdDiscipline versIdDiscipline(String valeur) {
        try {
            return IdDiscipline.valueOf(valeur);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Discipline inconnue : " + valeur);
        }
    }
}