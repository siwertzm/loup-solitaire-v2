package com.loupsolitaire.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.CreerPersonnageRequest;
import com.loupsolitaire.backend.response.InventaireItemResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.InventaireService;
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
    private final InventaireService inventaireService;

    @PostMapping
    public ResponseEntity<PersonnageResponse> creer(
            @Valid @RequestBody CreerPersonnageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Utilisateur utilisateur = recupererUtilisateur(userDetails);

        List<IdDiscipline> disciplines = request.getDisciplines().stream()
                .map(this::versIdDiscipline)
                .toList();

        Personnage personnage = personnageService.creerPersonnage(utilisateur, request.getNom(), disciplines);

        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(personnage));
    }

    // Ecran profil : la liste des personnages de l'utilisateur connecte.
    @GetMapping
    public List<PersonnageResponse> lister(@AuthenticationPrincipal UserDetails userDetails) {
        Utilisateur utilisateur = recupererUtilisateur(userDetails);

        return personnageRepository.findByUtilisateur(utilisateur).stream()
                .map(this::versReponse)
                .toList();
    }

    // Fiche personnage complete (reprise d'une partie).
    @GetMapping("/{id}")
    public PersonnageResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Personnage personnage = personnageRepository.findById(id)
                .orElseThrow(() -> new RessourceNonTrouveeException("Personnage introuvable : " + id));

        if (!personnage.getUtilisateur().getUsername().equals(userDetails.getUsername())) {
            throw new AccesRefuseException("Ce personnage ne vous appartient pas");
        }

        return versReponse(personnage);
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

    private PersonnageResponse versReponse(Personnage personnage) {
        List<InventaireItemResponse> inventaire = inventaireService.listerInventaire(personnage).stream()
                .map(this::versReponseInventaire)
                .toList();

        return new PersonnageResponse(
                personnage.getId(),
                personnage.getNom(),
                personnage.getHabilite(),
                personnage.getEnduranceMax(),
                personnage.getEnduranceActuelle(),
                personnage.getDisciplines().stream().map(d -> d.getId().name()).toList(),
                personnage.getArmeMaitrisee() != null ? personnage.getArmeMaitrisee().getNom() : null,
                personnage.getChapitreActuel().getId(),
                inventaire
        );
    }

    private InventaireItemResponse versReponseInventaire(InventaireItem item) {
        return new InventaireItemResponse(
                item.getObjet().getId(),
                item.getObjet().getNom(),
                item.getObjet().getCategorie().name(),
                item.getQuantite()
        );
    }
}