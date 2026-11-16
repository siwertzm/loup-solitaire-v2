package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.service.PartieService;

import jakarta.persistence.EntityManagerFactory;

/**
 * OPS-07 : nombre de requetes SQL des actions de jeu les plus frequentes,
 * mesure par les statistiques Hibernate sur PostgreSQL.
 */
class RequetesParActionIT extends IntegrationPostgres {

    @Autowired
    private PartieService partieService;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // Sans Chasse : l'effet Repas consomme vraiment un Repas.
    private static final List<IdDiscipline> DISCIPLINES = List.of(
            IdDiscipline.CAMOUFLAGE, IdDiscipline.BOUCLIER_PSYCHIQUE, IdDiscipline.SIXIEME_SENS,
            IdDiscipline.ORIENTATION, IdDiscipline.GUERISON);

    private Utilisateur utilisateur;
    private UtilisateurConnecte connecte;
    private UUID personnageId;

    @BeforeEach
    void creerUnPersonnage() {
        utilisateur = new Utilisateur();
        utilisateur.setUsername("ops07-" + UUID.randomUUID().toString().substring(0, 8));
        utilisateur.setEmail(UUID.randomUUID() + "@example.com");
        utilisateur.setPassword("hash");
        utilisateur.setEmailVerifie(true);
        utilisateur.setDateCreation(Instant.now());
        utilisateur = utilisateurRepository.save(utilisateur);
        connecte = new UtilisateurConnecte(utilisateur.getId());

        partieService.tirerCaracteristiques(connecte);
        personnageId = partieService.creerPersonnage(connecte, "Loup", DISCIPLINES).id();
    }

    @AfterEach
    void nettoyer() {
        partieService.listerPersonnages(connecte)
                .forEach(p -> partieService.supprimerPersonnage(p.id(), connecte));
        jdbc.update("delete from utilisateur where id = ?", utilisateur.getId());
    }

    private long requetes(Runnable action) {
        // Cache des chapitres vide : mesure stable quel que soit l'ordre des tests.
        entityManagerFactory.getCache().evictAll();
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        action.run();
        return stats.getPrepareStatementCount();
    }

    private long avancer(int depuis, int vers) {
        jdbc.update("update personnage set chapitre_actuel_id = ? where id = ?", depuis, personnageId);
        return requetes(() -> partieService.avancerVersChapitre(personnageId, vers, connecte));
    }

    // Plafonds = mesures du 16/11/2026, cache des chapitres vide (pire cas,
    // juste apres un redemarrage). Un depassement signale une requete en
    // trop, typiquement un N+1.
    @Test
    void avancerVersUnChapitreSimple() {
        assertThat(avancer(1, 85)).isLessThanOrEqualTo(11);
    }

    @Test
    void avancerVersUnChapitreAvecRepas() {
        assertThat(avancer(82, 235)).isLessThanOrEqualTo(14);
    }

    // Le plus lourd : 3 objets dont 4 Repas obligatoires, puis un Repas
    // consomme. 5 des 21 requetes sont des ecritures.
    @Test
    void avancerVersLe184() {
        assertThat(avancer(24, 184)).isLessThanOrEqualTo(21);
    }

    @Test
    void afficherUnChapitre() {
        jdbc.update("update personnage set chapitre_actuel_id = 184 where id = ?", personnageId);
        assertThat(requetes(() -> partieService.chapitreCourant(personnageId, connecte))).isLessThanOrEqualTo(11);
    }

    @Test
    void listerLesPersonnagesNeFaitPasUneRequeteParPersonnage() {
        for (int i = 0; i < 3; i++) {
            partieService.tirerCaracteristiques(connecte);
            partieService.creerPersonnage(connecte, "Loup" + i, DISCIPLINES);
        }
        assertThat(requetes(() -> partieService.listerPersonnages(connecte))).isEqualTo(3);
    }
}
