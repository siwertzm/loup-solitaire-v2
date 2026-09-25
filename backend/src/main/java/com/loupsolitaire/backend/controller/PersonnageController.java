package com.loupsolitaire.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.request.CreerPersonnageRequest;
import com.loupsolitaire.backend.response.ChapitreParcouruResponse;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.TirageResponse;
import com.loupsolitaire.backend.service.PartieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Couche HTTP uniquement : traduit la requete (chemin, corps, utilisateur
// connecte) et delegue a PartieService, qui gere la transaction, la
// verification du proprietaire et les regles du jeu.
@RestController
@RequestMapping("/personnages")
@RequiredArgsConstructor
public class PersonnageController {

    private final PartieService partieService;

    // Tirage des caracteristiques (HABILETE = 10 + chiffre, ENDURANCE = 20 +
    // chiffre), fait par le serveur. Appels repetes : toujours le meme
    // tirage, jusqu'a la creation du personnage.
    @PostMapping("/tirage")
    public TirageResponse tirer(@AuthenticationPrincipal UtilisateurConnecte connecte) {
        return partieService.tirerCaracteristiques(connecte);
    }

    // Utilise le tirage en attente ; 400 si aucun tirage n'a ete fait.
    @PostMapping
    public ResponseEntity<PersonnageResponse> creer(
            @Valid @RequestBody CreerPersonnageRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        List<IdDiscipline> disciplines = request.getDisciplines().stream()
                .map(this::versIdDiscipline)
                .toList();

        PersonnageResponse reponse = partieService.creerPersonnage(connecte, request.getNom(), disciplines);

        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    // Ecran profil/accueil : personnages de l'utilisateur connecte, le plus
    // recemment joue en tete.
    @GetMapping
    public List<PersonnageResponse> lister(@AuthenticationPrincipal UtilisateurConnecte connecte) {
        return partieService.listerPersonnages(connecte);
    }

    // Fiche personnage complete (reprise d'une partie).
    @GetMapping("/{id}")
    public PersonnageResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {
        return partieService.recupererPersonnage(id, connecte);
    }

    // Chapitre courant du personnage, en lecture seule.
    @GetMapping("/{id}/chapitre")
    public ChapitreResponse chapitreCourant(@PathVariable UUID id, @AuthenticationPrincipal UtilisateurConnecte connecte) {
        return partieService.chapitreCourant(id, connecte);
    }

    // Avance le personnage vers chapitreCibleId, si un lien valide (avec
    // conditions remplies) existe depuis son chapitre actuel.
    @PostMapping("/{id}/chapitre/{chapitreCibleId}")
    public PersonnageResponse avancerVersChapitre(
            @PathVariable UUID id,
            @PathVariable Integer chapitreCibleId,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.avancerVersChapitre(id, chapitreCibleId, connecte);
    }

    // MVP1 : apres une DEFAITE en combat, paye 1 coin pour revenir au
    // chapitre precedent (endurance entierement restauree). Nom de route
    // distinct de "/chapitre/{chapitreCibleId}" (litteral vs variable
    // Integer) : Spring privilegie toujours le match litteral, pas de
    // conflit de routage.
    @PostMapping("/{id}/chapitre/revenir-apres-defaite")
    public PersonnageResponse revenirApresDefaite(
            @PathVariable UUID id,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.revenirApresDefaite(id, connecte);
    }

    // MVP1 : seule action possible pour un personnage mort HORS combat.
    // Ne change pas de chapitre : restaure l'endurance et repasse mort a false.
    @PostMapping("/{id}/ressusciter")
    public PersonnageResponse ressusciter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.ressusciter(id, connecte);
    }

    // Ramasse un objet propose par le chapitre courant. La quantite vient
    // toujours du chapitre lui-meme, jamais du client.
    @PostMapping("/{id}/objets/{objetId}")
    public PersonnageResponse ajouterObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.ramasserObjet(id, objetId, connecte);
    }

    // Retire un objet possede, sans jamais appliquer d'effet de consommable
    // (voir /consommer ci-dessous).
    @DeleteMapping("/{id}/objets/{objetId}")
    public PersonnageResponse retirerObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @RequestParam(defaultValue = "1") int quantite,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.retirerObjet(id, objetId, quantite, connecte);
    }

    // Consomme 1 exemplaire d'un objet (potion, laumspur...) : applique son
    // effet PUIS le retire de l'inventaire.
    @PostMapping("/{id}/objets/{objetId}/consommer")
    public PersonnageResponse consommerObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.consommerObjet(id, objetId, connecte);
    }

    // Resout un vol en attente : le joueur choisit quel objet il perd.
    @PostMapping("/{id}/vol/{objetId}")
    public PersonnageResponse resoudreVol(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.resoudreVol(id, objetId, connecte);
    }

    // Echange volontaire (ex. chapitre 307), valide cote serveur contre les
    // echanges proposes par le chapitre ACTUEL.
    @PostMapping("/{id}/objets/{objetAAjouterId}/echanger-contre/{objetARetirerId}")
    public PersonnageResponse echangerObjet(
            @PathVariable UUID id,
            @PathVariable String objetAAjouterId,
            @PathVariable String objetARetirerId,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.echangerObjet(id, objetAAjouterId, objetARetirerId, connecte);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        partieService.supprimerPersonnage(id, connecte);
    }

    // Journal du parcours : chapitres traverses, du plus recent au plus
    // ancien (le premier est donc le chapitre courant).
    @GetMapping("/{id}/historique")
    public List<ChapitreParcouruResponse> historique(
            @PathVariable UUID id,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {

        return partieService.historique(id, connecte);
    }

    private IdDiscipline versIdDiscipline(String valeur) {
        try {
            return IdDiscipline.valueOf(valeur);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Discipline inconnue : " + valeur);
        }
    }
}