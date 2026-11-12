package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * SEC-08 : deux inscriptions simultanees avec le meme email. Les deux passent
 * la verification existsByEmail, la base refuse la seconde (contrainte
 * uk_utilisateur_email) : 409, jamais 500.
 *
 * La course est rendue certaine : la premiere inscription est une
 * transaction SQL ouverte et pas encore validee. La seconde (requete HTTP) ne
 * la voit pas pendant sa verification, puis son INSERT attend le verrou de
 * l'index unique ; la premiere est alors validee et la seconde echoue.
 */
class InscriptionSimultaneeIT extends IntegrationPostgres {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;
    private final String email = "course-" + UUID.randomUUID() + "@test.local";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("delete from email_verification_token where utilisateur_id in "
                + "(select id from utilisateur where email = ?)", email);
        jdbc.update("delete from utilisateur where email = ?", email);
    }

    @Test
    void laSecondeInscriptionAvecLeMemeEmailRecoit409() throws Exception {
        try (Connection premiere = dataSource.getConnection()) {
            premiere.setAutoCommit(false);
            try (PreparedStatement insert = premiere.prepareStatement("""
                    insert into utilisateur (id, date_creation, email, email_verifie, password, username)
                    values (?, now(), ?, false, 'x', ?)
                    """)) {
                insert.setObject(1, UUID.randomUUID());
                insert.setString(2, email);
                insert.setString(3, "premier-" + UUID.randomUUID().toString().substring(0, 8));
                insert.executeUpdate();
            }

            CompletableFuture<MvcResult> seconde = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(post("/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"username": "%s", "email": "%s", "password": "motdepasse-sec08"}
                                            """.formatted("second-" + UUID.randomUUID().toString().substring(0, 8), email)))
                            .andReturn();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            attendreQueLaSecondeSoitBloquee();
            premiere.commit();

            MvcResult reponse = seconde.get(30, TimeUnit.SECONDS);
            assertThat(reponse.getResponse().getStatus()).isEqualTo(409);
            assertThat(reponse.getResponse().getContentAsString())
                    .contains("Conflit")
                    .doesNotContain("uk_utilisateur");
        }
        assertThat(jdbc.queryForObject("select count(*) from utilisateur where email = ?", Long.class, email))
                .isEqualTo(1);
    }

    // L'INSERT de la seconde inscription attend le verrou de l'index unique.
    private void attendreQueLaSecondeSoitBloquee() throws InterruptedException {
        Instant limite = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(limite)) {
            Long bloquees = jdbc.queryForObject("""
                    select count(*) from pg_stat_activity
                    where wait_event_type = 'Lock' and query ilike 'insert into utilisateur%'
                    """, Long.class);
            if (bloquees != null && bloquees > 0) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
        throw new AssertionError("La seconde inscription n'a jamais attendu le verrou");
    }
}
