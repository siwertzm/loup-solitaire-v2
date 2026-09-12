package com.loupsolitaire.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
import com.loupsolitaire.backend.service.EffetChapitreService;
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
    private final EffetChapitreService effetChapitreService;
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

        Personnage personnage = personnageService.creerPersonnage(utilisateur, request.getNom(), disciplines, request.getHasardHabilite(), request.getHasardEndurance());

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
    @Transactional
    public PersonnageResponse recuperer(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        return personnageMapper.versReponse(personnage);
    }

    // Chapitre courant du personnage, en lecture seule. Le tirage HASARD
    // (Personnage.dernierTirageHasard) est fige depuis l'arrivee sur ce
    // chapitre (voir PersonnageService.avancerVersChapitre) : consulter cet
    // ecran plusieurs fois ne le fait jamais changer.
    @GetMapping("/{id}/chapitre")
    @Transactional
    public ChapitreResponse chapitreCourant(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        return chapitreMapper.versReponse(personnage.getChapitreActuel().getId(), personnage);
    }

    // Avance le personnage vers chapitreCibleId, si un lien valide (avec
    // conditions remplies) existe depuis son chapitre actuel. Ne fait
    // qu'avancer le pointeur (chapitrePrecedent/chapitreActuel) et
    // reinitialiser l'habilite temporaire : n'applique pas encore les
    // effets/ennemis/objets du nouveau chapitre (etape suivante).
    @PostMapping("/{id}/chapitre/{chapitreCibleId}")
    @Transactional
    public PersonnageResponse avancerVersChapitre(
            @PathVariable UUID id,
            @PathVariable Integer chapitreCibleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        personnageService.avancerVersChapitre(personnage, chapitreCibleId);

        return personnageMapper.versReponse(personnage);
    }

    // MVP1 : apres une DEFAITE en combat, paye 1 coin pour revenir au
    // chapitre precedent (endurance entierement restauree). Nom de route
    // distinct de "/chapitre/{chapitreCibleId}" (litteral vs variable
    // Integer) : Spring privilegie toujours le match litteral, pas de
    // conflit de routage.
    @PostMapping("/{id}/chapitre/revenir-apres-defaite")
    @Transactional
    public PersonnageResponse revenirApresDefaite(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        personnageService.revenirApresDefaite(personnage);

        return personnageMapper.versReponse(personnage);
    }

    // MVP1 : seule action possible pour un personnage mort HORS combat
    // (perte d'endurance par un effet de chapitre, voir Personnage.mort).
    // Distinct de revenir-apres-defaite : ne change pas de chapitre, paye
    // 1 coin (non retire, illimite pour le MVP1) pour restaurer
    // l'endurance a fond et repasser mort a false.
    @PostMapping("/{id}/ressusciter")
    @Transactional
    public PersonnageResponse ressusciter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        personnageService.ressusciter(personnage);

        return personnageMapper.versReponse(personnage);
    }

    // Ramasse un objet optionnel propose par le chapitre courant. Securise
    // cote serveur (PersonnageService.ramasserObjetDuChapitre) : impossible
    // de ramasser un objet non propose ici, et la quantite vient toujours
    // du chapitre lui-meme, jamais du client.
    @PostMapping("/{id}/objets/{objetId}")
    @Transactional
    public PersonnageResponse ajouterObjet(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objet = recupererObjet(objetId);

        personnageService.ramasserObjetDuChapitre(personnage, objet);

        return personnageMapper.versReponse(personnage);
    }

    // Endpoint de test/debug symetrique : retire un objet possede, sans
    // jamais appliquer d'effet de consommable (voir /consommer ci-dessous).
    @DeleteMapping("/{id}/objets/{objetId}")
    @Transactional
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
    @Transactional
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

    // Resout un vol en attente (Effet VOL, valeur=1) : le joueur choisit
    // quel objet/repas/arme il perd, parmi ceux autorises par la portee
    // (voir PersonnageResponse.volEnAttente pour savoir si un choix est
    // requis, et lequel).
    @PostMapping("/{id}/vol/{objetId}")
    @Transactional
    public PersonnageResponse resoudreVol(
            @PathVariable UUID id,
            @PathVariable String objetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objet = recupererObjet(objetId);

        effetChapitreService.resoudreVolEnAttente(personnage, objet);

        return personnageMapper.versReponse(personnage);
    }

    // Echange volontaire (jamais force) : retire objetARetirerId pour
    // ajouter objetAAjouterId a la place. Valide cote serveur que le
    // chapitre ACTUEL du personnage propose bien cet echange precis (Effet
    // ECHANGE) : impossible d'echanger n'importe quoi n'importe ou.
    // Cas d'usage : chapitre 307 (Marteau de Guerre de l'ermite).
    @PostMapping("/{id}/objets/{objetAAjouterId}/echanger-contre/{objetARetirerId}")
    @Transactional
    public PersonnageResponse echangerObjet(
            @PathVariable UUID id,
            @PathVariable String objetAAjouterId,
            @PathVariable String objetARetirerId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
        Objet objetAAjouter = recupererObjet(objetAAjouterId);
        Objet objetARetirer = recupererObjet(objetARetirerId);

        personnageService.echangerObjet(personnage, objetARetirer, objetAAjouter);

        return personnageMapper.versReponse(personnage);
    }

    /**
     * Supprime un personnage.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

    Personnage personnage = recupererEtVerifierProprietaire(id, userDetails);
    personnageService.supprimerPersonnage(personnage);
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