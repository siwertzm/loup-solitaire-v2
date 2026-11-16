package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.InventaireItemRepository;
import com.loupsolitaire.backend.repository.ObjetChapitreRamasseRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.TirageCreationRepository;
import com.loupsolitaire.backend.response.ExportDonneesResponse;
import com.loupsolitaire.backend.response.ExportDonneesResponse.CombatExporte;
import com.loupsolitaire.backend.response.ExportDonneesResponse.Compte;
import com.loupsolitaire.backend.response.ExportDonneesResponse.ObjetPossede;
import com.loupsolitaire.backend.response.ExportDonneesResponse.ObjetRamasse;
import com.loupsolitaire.backend.response.ExportDonneesResponse.PersonnageExporte;
import com.loupsolitaire.backend.response.ExportDonneesResponse.TirageCreationEnCours;

import lombok.RequiredArgsConstructor;

/**
 * RGPD-02 : rassemble toutes les donnees d'un compte pour l'export (voir
 * ExportDonneesResponse pour ce qui est volontairement exclu).
 */
@Service
@RequiredArgsConstructor
public class ExportDonneesService {

    private final PersonnageRepository personnageRepository;
    private final InventaireItemRepository inventaireItemRepository;
    private final ObjetChapitreRamasseRepository objetChapitreRamasseRepository;
    private final CombatRepository combatRepository;
    private final TirageCreationRepository tirageCreationRepository;

    @Transactional(readOnly = true)
    public ExportDonneesResponse construire(Utilisateur utilisateur) {
        Compte compte = new Compte(
                utilisateur.getUsername(),
                utilisateur.getEmail(),
                utilisateur.isEmailVerifie(),
                utilisateur.getEmailEnAttente(),
                utilisateur.getDateCreation());

        TirageCreationEnCours tirage = tirageCreationRepository.findById(utilisateur.getId())
                .map(t -> new TirageCreationEnCours(t.getHasardHabilite(), t.getHasardEndurance(), t.getCreeLe()))
                .orElse(null);

        List<PersonnageExporte> personnages = personnageRepository.findByUtilisateur(utilisateur).stream()
                .map(this::exporter)
                .toList();

        return new ExportDonneesResponse(Instant.now(), compte, tirage, personnages);
    }

    private PersonnageExporte exporter(Personnage p) {
        List<ObjetPossede> inventaire = inventaireItemRepository.findByPersonnageId(p.getId()).stream()
                .map(item -> new ObjetPossede(item.getObjet().getNom(), item.getQuantite()))
                .toList();

        List<ObjetRamasse> ramasses = objetChapitreRamasseRepository.findByPersonnageId(p.getId()).stream()
                .map(r -> new ObjetRamasse(r.getChapitre().getId(), r.getObjet().getNom(), r.getQuantite()))
                .toList();

        List<CombatExporte> combats = combatRepository.findByPersonnage(p).stream()
                .map(c -> new CombatExporte(
                        c.getChapitreId(),
                        c.getStatut().name(),
                        c.getAssautsLivres(),
                        c.getEnnemis().stream().map(ce -> ce.getEnnemi().getNom()).toList(),
                        c.getCreeLe()))
                .toList();

        return new PersonnageExporte(
                p.getId(),
                p.getNom(),
                p.getDateCreation(),
                p.getDerniereActivite(),
                p.getHabiliteBase(),
                p.getHabilite(),
                p.getEnduranceMax(),
                p.getEnduranceActuelle(),
                p.getDisciplines().stream().map(d -> d.getId().name()).toList(),
                p.getArmeMaitrisee() != null ? p.getArmeMaitrisee().getNom() : null,
                idChapitre(p.getChapitreActuel()),
                idChapitre(p.getChapitrePrecedent()),
                p.isMort(),
                List.copyOf(p.getChapitresParcourus()),
                inventaire,
                ramasses,
                combats);
    }

    private static Integer idChapitre(Chapitre chapitre) {
        return chapitre != null ? chapitre.getId() : null;
    }
}
