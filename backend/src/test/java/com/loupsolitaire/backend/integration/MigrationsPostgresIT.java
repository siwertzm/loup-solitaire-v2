package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * QUAL-01 : les migrations Liquibase s'appliquent sur une base PostgreSQL
 * vide, et le resultat est celui qu'attend le code.
 *
 * Le simple demarrage de ce test prouve deja deux choses : toutes les
 * migrations passent sur PostgreSQL 16 (le SQL de H2 est plus permissif), et
 * Hibernate (ddl-auto=validate) trouve chaque table et chaque colonne des
 * entites. Les tests ci-dessous verifient le reste.
 */
class MigrationsPostgresIT extends IntegrationPostgres {

    @Test
    void toutesLesMigrationsDuFichierMaitreOntEteAppliquees() throws IOException {
        long attendu = nombreDeChangelogsInclus();

        Long appliques = jdbc.queryForObject("select count(*) from databasechangelog", Long.class);

        assertThat(appliques).isEqualTo(attendu);
    }

    @Test
    void leCatalogueDuLivreEstChargeApresLesMigrations() {
        // GameDataLoader passe apres Liquibase, comme sur une base neuve.
        assertThat(jdbc.queryForObject("select count(*) from chapitre where id in (1, 350)", Long.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForList(
                "select habilite || '/' || endurance from ennemi where id = 'serpent_aile'", String.class))
                .containsExactly("16/18");
    }

    @Test
    void laDateDeNaissanceNExistePlusEnBase() {
        // RGPD-01 (migration 013).
        Long colonnes = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = 'utilisateur'
                  and column_name = 'date_naissance'
                """, Long.class);

        assertThat(colonnes).isZero();
    }

    @Test
    void lOrdreDesEnnemisVientDeLaColonneOrdre() {
        // REGLE-08 (migration 014) : colonne obligatoire, remplie dans
        // l'ordre du livre.
        String nullable = jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                where table_schema = 'public' and table_name = 'chapitre_ennemi'
                  and column_name = 'ordre'
                """, String.class);
        List<String> section180 = jdbc.queryForList(
                "select ennemi_id from chapitre_ennemi where chapitre_id = 180 order by ordre", String.class);

        assertThat(nullable).isEqualTo("NO");
        assertThat(section180).containsExactly("chef_soldats", "soldat_premier", "soldat_deuxieme");
    }

    /** Nombre de "- include:" du fichier maitre (un changeset par fichier). */
    private static long nombreDeChangelogsInclus() throws IOException {
        try (InputStream in = MigrationsPostgresIT.class
                .getResourceAsStream("/db/changelog/db.changelog-master.yaml")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(ligne -> ligne.trim().equals("- include:"))
                    .count();
        }
    }
}