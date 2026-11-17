package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.service.PartieService;

/**
 * QUAL-20 : toute action de jeu met a jour derniereActivite (personnage a
 * reprendre sur l'accueil), meme si elle ne touche que l'inventaire.
 */
class DerniereActiviteIT extends IntegrationPostgres {

    private static final Instant ANCIENNE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private PartieService partieService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private Utilisateur utilisateur;
    private UtilisateurConnecte connecte;
    private UUID personnageId;

    @BeforeEach
    void creerUnPersonnageAuChapitre20() {
        utilisateur = new Utilisateur();
        utilisateur.setUsername("qual20-" + UUID.randomUUID().toString().substring(0, 8));
        utilisateur.setEmail(UUID.randomUUID() + "@example.com");
        utilisateur.setPassword("hash");
        utilisateur.setEmailVerifie(true);
        utilisateur.setDateCreation(Instant.now());
        utilisateur = utilisateurRepository.save(utilisateur);
        connecte = new UtilisateurConnecte(utilisateur.getId());

        partieService.tirerCaracteristiques(connecte);
        personnageId = partieService.creerPersonnage(connecte, "Loup", List.of(
                IdDiscipline.CAMOUFLAGE, IdDiscipline.BOUCLIER_PSYCHIQUE, IdDiscipline.SIXIEME_SENS,
                IdDiscipline.ORIENTATION, IdDiscipline.GUERISON)).id();
        // Chapitre 20 : Repas optionnels a ramasser, sans combat ni effet.
        jdbc.update("update personnage set chapitre_actuel_id = 20, derniere_activite = ? where id = ?",
                Timestamp.from(ANCIENNE), personnageId);
    }

    @AfterEach
    void nettoyer() {
        partieService.supprimerPersonnage(personnageId, connecte);
        jdbc.update("delete from utilisateur where id = ?", utilisateur.getId());
    }

    private Instant derniereActivite() {
        return jdbc.queryForObject("select derniere_activite from personnage where id = ?",
                Timestamp.class, personnageId).toInstant();
    }

    @Test
    void ramasserUnObjetMetAJourLaDerniereActivite() {
        Instant avant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        partieService.ramasserObjet(personnageId, "repas", connecte);

        assertThat(derniereActivite()).isAfterOrEqualTo(avant);
    }

    @Test
    void uneLectureNeChangePasLaDerniereActivite() {
        partieService.recupererPersonnage(personnageId, connecte);
        partieService.chapitreCourant(personnageId, connecte);

        assertThat(derniereActivite()).isEqualTo(ANCIENNE);
    }
}
