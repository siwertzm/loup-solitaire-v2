package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.mail.internet.MimeMessage;

/**
 * OPS-02 de bout en bout, sur PostgreSQL : l'inscription repond sans
 * attendre le serveur SMTP, et aucune connexion a la base n'est gardee
 * pendant l'envoi de l'email.
 *
 * Le SMTP simule reste bloque tant que le test ne le libere pas : si
 * l'envoi se faisait dans la requete, /auth/register ne repondrait jamais
 * avant la fin du delai.
 */
class EmailAsynchroneIT extends IntegrationPostgres {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;
    private CountDownLatch smtpLibere;
    private CountDownLatch smtpAppele;
    private String email;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        smtpLibere = new CountDownLatch(1);
        smtpAppele = new CountDownLatch(1);
        doAnswer(invocation -> {
            smtpAppele.countDown();
            smtpLibere.await(30, TimeUnit.SECONDS);
            return null;
        }).when(serveurSmtp).send(any(MimeMessage.class));
        email = "ops02-" + UUID.randomUUID() + "@example.com";
    }

    @AfterEach
    void tearDown() {
        smtpLibere.countDown();
        jdbc.update("""
                delete from email_verification_token
                where utilisateur_id in (select id from utilisateur where email = ?)
                """, email);
        jdbc.update("delete from utilisateur where email = ?", email);
    }

    @Test
    void lInscriptionRepondSansAttendreLeServeurSmtp() throws Exception {
        String corps = """
                {"username": "%s", "email": "%s", "password": "motdepasse-ops02"}
                """.formatted("ops02-" + UUID.randomUUID().toString().substring(0, 8), email);

        long debut = System.nanoTime();
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());
        Duration duree = Duration.ofNanos(System.nanoTime() - debut);

        // L'email est bien parti vers le SMTP, qui est encore bloque...
        assertThat(smtpAppele.await(10, TimeUnit.SECONDS)).isTrue();
        // ... pourtant la requete a deja repondu. Seuil large (BCrypt, base,
        // machine de CI lente) : l'important est de ne pas attendre le SMTP.
        assertThat(duree).isLessThan(Duration.ofSeconds(1));
        // Et le fil d'envoi ne tient aucune connexion a la base.
        assertThat(dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections())
                .isZero();

        smtpLibere.countDown();
        verify(serveurSmtp, timeout(5_000)).send(any(MimeMessage.class));
    }
}