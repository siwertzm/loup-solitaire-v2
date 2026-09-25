package com.loupsolitaire.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;

/**
 * Verifie le VRAI mecanisme de verrou optimiste, avec une base (H2) et de
 * vraies transactions : c'est la base qui refuse la seconde validation,
 * aucun mock ne peut le simuler.
 *
 * Scenario "double tap" : la requete A charge le personnage ; pendant
 * qu'elle travaille, la requete B charge le meme personnage et valide.
 * A valide ensuite sur une version perimee et doit etre rejetee.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.datasource.hikari.connection-init-sql="
})
class PersonnageVerrouOptimisteTest {

    @Autowired
    private PersonnageRepository personnageRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private ChapitreRepository chapitreRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private TransactionTemplate autreTransaction;

    private UUID utilisateurId;
    private UUID personnageId;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        // Transaction independante, validee avant la fin de la premiere :
        // simule une seconde requete HTTP traitee en parallele.
        autreTransaction = new TransactionTemplate(transactionManager);
        autreTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.executeWithoutResult(status -> {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setUsername("verrou-" + UUID.randomUUID());
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
            // Chapitre 0 charge au demarrage par GameDataLoader.
            personnage.setChapitreActuel(chapitreRepository.findById(0).orElseThrow());
            personnageId = personnageRepository.save(personnage).getId();
        });
    }

    @AfterEach
    void tearDown() {
        transaction.executeWithoutResult(status -> {
            personnageRepository.deleteById(personnageId);
            utilisateurRepository.deleteById(utilisateurId);
        });
    }

    private long versionActuelle() {
        return transaction.execute(status ->
                personnageRepository.findById(personnageId).orElseThrow().getVersion());
    }

    @Test
    void uneActionIncrementeLaVersionMemeSansModifierLePersonnage() {
        long avant = versionActuelle();

        // Aucune modification : seul le chargement "pour modification".
        transaction.executeWithoutResult(status ->
                personnageRepository.findByIdPourModification(personnageId).orElseThrow());

        assertThat(versionActuelle()).isEqualTo(avant + 1);
    }

    @Test
    void uneLectureSimpleNIncrementePasLaVersion() {
        long avant = versionActuelle();

        transaction.executeWithoutResult(status ->
                personnageRepository.findById(personnageId).orElseThrow());

        assertThat(versionActuelle()).isEqualTo(avant);
    }

    @Test
    void deuxActionsSimultaneesLaSecondeAValiderEstRejetee() {
        long avant = versionActuelle();

        assertThatThrownBy(() -> transaction.executeWithoutResult(requeteA -> {
            personnageRepository.findByIdPourModification(personnageId).orElseThrow();

            // Pendant que A travaille, B charge le meme personnage et valide.
            autreTransaction.executeWithoutResult(requeteB ->
                    personnageRepository.findByIdPourModification(personnageId).orElseThrow());

            // Fin de A : sa validation porte sur une version perimee.
        })).isInstanceOf(OptimisticLockingFailureException.class);

        // Seule l'action de B a ete appliquee.
        assertThat(versionActuelle()).isEqualTo(avant + 1);
    }
}