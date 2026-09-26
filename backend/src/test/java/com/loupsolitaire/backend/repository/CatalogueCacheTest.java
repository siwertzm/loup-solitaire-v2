package com.loupsolitaire.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;

import jakarta.persistence.EntityManagerFactory;

/**
 * Verifie que le catalogue du livre est bien garde en cache de second niveau
 * (voir les @Cache sur Chapitre, Lien, Objet... et application.properties),
 * avec la vraie configuration Hibernate + JCache/Caffeine.
 *
 * Utilise l'API JPA standard (EntityManagerFactory.getCache()) : pas besoin
 * d'activer les statistiques Hibernate.
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
class CatalogueCacheTest {

    @Autowired
    private ChapitreRepository chapitreRepository;
    @Autowired
    private ObjetRepository objetRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        // Part d'un cache vide pour que chaque test prouve ce qu'il affirme.
        entityManagerFactory.getCache().evictAll();
    }

    @Test
    void unChapitreLuResteEnCachePourLesRequetesSuivantes() {
        assertThat(entityManagerFactory.getCache().contains(Chapitre.class, 1)).isFalse();

        transaction.executeWithoutResult(status -> chapitreRepository.findById(1).orElseThrow());

        // Transaction terminee (comme une requete HTTP terminee) : le chapitre
        // est toujours disponible sans relire la base.
        assertThat(entityManagerFactory.getCache().contains(Chapitre.class, 1)).isTrue();
    }

    @Test
    void lesLiensDUnChapitreSontAussiMisEnCache() {
        UUID premierLienId = transaction.execute(status -> {
            Chapitre chapitre = chapitreRepository.findById(1).orElseThrow();
            // Parcourt les liens et leurs conditions, comme ChapitreMapper.
            chapitre.getLiens().forEach(lien -> lien.getConditions().size());
            return chapitre.getLiens().getFirst().getId();
        });

        assertThat(entityManagerFactory.getCache().contains(Lien.class, premierLienId)).isTrue();
    }

    @Test
    void unObjetLuResteEnCache() {
        transaction.executeWithoutResult(status -> objetRepository.findById("repas").orElseThrow());

        assertThat(entityManagerFactory.getCache().contains(Objet.class, "repas")).isTrue();
    }
}