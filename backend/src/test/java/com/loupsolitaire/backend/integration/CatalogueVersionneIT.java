package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.loupsolitaire.backend.config.GameDataLoader;

import jakarta.persistence.EntityManagerFactory;

/**
 * DATA-00 : le catalogue du livre suit l'empreinte des JSON, sur PostgreSQL.
 * Empreinte inchangee : rien n'est recharge. Empreinte differente : tout est
 * remis a l'identique des JSON, sans toucher aux donnees des joueurs.
 */
class CatalogueVersionneIT extends IntegrationPostgres {

    private static final String TEXTE_ABIME = "texte abime";

    @Autowired
    private GameDataLoader gameDataLoader;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final UUID utilisateurId = UUID.randomUUID();
    private final UUID personnageId = UUID.randomUUID();

    @BeforeEach
    void unJoueurEnCoursDePartie() {
        jdbc.update("""
                insert into utilisateur (id, date_creation, email, email_verifie, password, username)
                values (?, now(), ?, true, 'x', ?)
                """, utilisateurId, utilisateurId + "@test.local", "joueur-" + utilisateurId);
        jdbc.update("""
                insert into personnage (id, date_creation, endurance_actuelle, endurance_max, habilite,
                    habilite_base, habilite_temp, mort, nom, arme_maitrisee_id, chapitre_actuel_id,
                    chapitre_precedent_id, utilisateur_id)
                values (?, now(), 20, 25, 15, 15, 0, false, 'Loup', 'epee', 20, 1, ?)
                """, personnageId, utilisateurId);
        jdbc.update("insert into inventaire_item (id, quantite, objet_id, personnage_id) values (?, 1, 'casque', ?)",
                UUID.randomUUID(), personnageId);
        jdbc.update("insert into personnage_chapitre_parcouru (personnage_id, ordre, chapitre_id) values (?, 0, 1)",
                personnageId);
    }

    @AfterEach
    void supprimerLeJoueurEtRemettreLeCatalogue() throws Exception {
        jdbc.update("delete from personnage_chapitre_parcouru where personnage_id = ?", personnageId);
        jdbc.update("delete from inventaire_item where personnage_id = ?", personnageId);
        jdbc.update("delete from personnage where id = ?", personnageId);
        jdbc.update("delete from utilisateur where id = ?", utilisateurId);
        // Les autres tests partagent cette base : catalogue remis d'apres les JSON.
        jdbc.update("update catalogue_empreinte set empreinte = 'a-recharger'");
        abimer(() -> { });
        gameDataLoader.run(null);
    }

    // Modifie la base en SQL, hors Hibernate : son cache de second niveau
    // est vide ensuite, comme apres un redemarrage.
    private void abimer(Runnable sql) {
        sql.run();
        entityManagerFactory.getCache().evictAll();
    }

    private void abimerLeCatalogue() {
        abimer(() -> {
            jdbc.update("update chapitre set text = ? where id = 1", TEXTE_ABIME);
            jdbc.update("update ennemi set habilite = 1 where id = 'serpent_aile'");
            jdbc.update("delete from cond where lien_id in (select id from lien where chapitre_id = 1)");
            jdbc.update("delete from lien where chapitre_id = 1");
        });
    }

    @Test
    void lEmpreinteDesJsonEstEnregistreeAuDemarrage() {
        assertThat(jdbc.queryForObject("select empreinte from catalogue_empreinte where id = 1", String.class))
                .hasSize(64);
    }

    @Test
    void sansChangementDesJsonRienNEstRecharge() throws Exception {
        abimerLeCatalogue();

        gameDataLoader.run(null);

        assertThat(jdbc.queryForObject("select text from chapitre where id = 1", String.class))
                .isEqualTo(TEXTE_ABIME);
        assertThat(jdbc.queryForObject("select count(*) from lien where chapitre_id = 1", Long.class))
                .isZero();
    }

    @Test
    void siLEmpreinteChangeLeCatalogueRedevientCeluiDesJson() throws Exception {
        String empreinte = jdbc.queryForObject("select empreinte from catalogue_empreinte", String.class);
        long liensAvant = jdbc.queryForObject("select count(*) from lien", Long.class);
        long condsAvant = jdbc.queryForObject("select count(*) from cond", Long.class);
        long effetsAvant = jdbc.queryForObject("select count(*) from effet", Long.class);
        abimerLeCatalogue();
        jdbc.update("update catalogue_empreinte set empreinte = 'ancienne'");

        gameDataLoader.run(null);

        assertThat(jdbc.queryForObject("select text from chapitre where id = 1", String.class))
                .isNotEqualTo(TEXTE_ABIME);
        assertThat(jdbc.queryForObject("select habilite from ennemi where id = 'serpent_aile'", Integer.class))
                .isEqualTo(16);
        assertThat(jdbc.queryForObject("select count(*) from lien where chapitre_id = 1", Long.class))
                .isPositive();
        // Enfants recrees, sans doublon.
        assertThat(jdbc.queryForObject("select count(*) from lien", Long.class)).isEqualTo(liensAvant);
        assertThat(jdbc.queryForObject("select count(*) from cond", Long.class)).isEqualTo(condsAvant);
        assertThat(jdbc.queryForObject("select count(*) from effet", Long.class)).isEqualTo(effetsAvant);
        assertThat(jdbc.queryForObject("select empreinte from catalogue_empreinte", String.class))
                .isEqualTo(empreinte);
    }

    @Test
    void leRechargementNeTouchePasAuxDonneesDesJoueurs() throws Exception {
        jdbc.update("update catalogue_empreinte set empreinte = 'ancienne'");

        gameDataLoader.run(null);

        assertThat(jdbc.queryForObject(
                "select chapitre_actuel_id || '/' || arme_maitrisee_id from personnage where id = ?",
                String.class, personnageId)).isEqualTo("20/epee");
        assertThat(jdbc.queryForObject(
                "select objet_id from inventaire_item where personnage_id = ?", String.class, personnageId))
                .isEqualTo("casque");
        assertThat(jdbc.queryForObject(
                "select chapitre_id from personnage_chapitre_parcouru where personnage_id = ?",
                Integer.class, personnageId)).isEqualTo(1);
    }
}
