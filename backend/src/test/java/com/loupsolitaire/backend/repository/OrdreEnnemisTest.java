package com.loupsolitaire.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.StatutCombat;

/**
 * REGLE-08, avec une vraie base (H2) : l'ordre des ennemis d'un chapitre et
 * d'un combat vient de la base (colonnes "ordre"), pas de l'ordre dans
 * lequel les lignes ont ete ecrites.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.datasource.hikari.connection-init-sql="
})
class OrdreEnnemisTest {

    @Autowired
    private ChapitreRepository chapitreRepository;
    @Autowired
    private CombatRepository combatRepository;
    @Autowired
    private EnnemiRepository ennemiRepository;
    @Autowired
    private PersonnageRepository personnageRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private UUID utilisateurId;
    private UUID personnageId;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setUsername("ordre-" + UUID.randomUUID());
            utilisateur.setEmail(UUID.randomUUID() + "@example.com");
            utilisateur.setPassword("hash");
            utilisateur.setDateCreation(Instant.now());
            utilisateurId = utilisateurRepository.save(utilisateur).getId();

            Personnage personnage = new Personnage();
            personnage.setUtilisateur(utilisateur);
            personnage.setNom("Loup");
            personnage.setHabiliteBase(15);
            personnage.setHabilite(15);
            personnage.setEnduranceMax(25);
            personnage.setEnduranceActuelle(25);
            personnage.setDateCreation(Instant.now());
            personnage.setChapitreActuel(chapitreRepository.findById(180).orElseThrow());
            personnageId = personnageRepository.save(personnage).getId();
        });
    }

    @AfterEach
    void tearDown() {
        transaction.executeWithoutResult(status -> {
            Personnage personnage = personnageRepository.findById(personnageId).orElseThrow();
            combatRepository.deleteAll(combatRepository.findByPersonnage(personnage));
            personnageRepository.delete(personnage);
            utilisateurRepository.deleteById(utilisateurId);
        });
    }

    private List<String> idsEnnemisDuChapitre(int chapitreId) {
        return transaction.execute(status -> chapitreRepository.findById(chapitreId).orElseThrow()
                .getEnnemis().stream().map(Ennemi::getId).toList());
    }

    @Test
    void lesEnnemisDUnChapitreSuiventLOrdreDuLivre() {
        assertThat(idsEnnemisDuChapitre(180))
                .containsExactly("chef_soldats", "soldat_premier", "soldat_deuxieme");
        assertThat(idsEnnemisDuChapitre(253))
                .containsExactly("loup_maudit_1", "loup_maudit_2", "loup_maudit_3", "loup_maudit_4");
    }

    @Test
    void lesEnnemisDUnCombatSontRelusDansLOrdreDAffrontement() {
        // Ecrits volontairement a l'envers : c'est la colonne "ordre" qui
        // doit decider, pas l'ordre d'insertion.
        transaction.executeWithoutResult(status -> {
            Personnage personnage = personnageRepository.findById(personnageId).orElseThrow();
            Combat combat = new Combat();
            combat.setPersonnage(personnage);
            combat.setChapitreId(180);
            combat.setStatut(StatutCombat.EN_COURS);
            combat.setCreeLe(Instant.now());

            List<CombatEnnemi> ennemis = new ArrayList<>();
            String[] ids = { "soldat_deuxieme", "soldat_premier", "chef_soldats" };
            for (int i = 0; i < ids.length; i++) {
                CombatEnnemi ce = new CombatEnnemi();
                ce.setCombat(combat);
                ce.setEnnemi(ennemiRepository.findById(ids[i]).orElseThrow());
                ce.setOrdre(ids.length - 1 - i);
                ce.setEnduranceActuelle(20);
                ennemis.add(ce);
            }
            combat.setEnnemis(ennemis);
            combatRepository.save(combat);
        });

        // Meme requete que CombatService (avec son @EntityGraph).
        List<String> relus = transaction.execute(status -> {
            Personnage personnage = personnageRepository.findById(personnageId).orElseThrow();
            return combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 180)
                    .orElseThrow()
                    .getEnnemis().stream().map(ce -> ce.getEnnemi().getId()).toList();
        });

        assertThat(relus).containsExactly("chef_soldats", "soldat_premier", "soldat_deuxieme");
    }
}