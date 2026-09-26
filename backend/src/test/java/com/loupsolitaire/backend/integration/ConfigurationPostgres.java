package com.loupsolitaire.backend.integration;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * QUAL-01 : infrastructure commune des tests d'integration (*IT).
 *
 * - Un vrai PostgreSQL 16 (meme image que docker-compose.yml) lance par
 *   Testcontainers ; @ServiceConnection branche la datasource dessus, a la
 *   place de DB_URL / DB_USERNAME / DB_PASSWORD.
 * - Un serveur SMTP simule (mock Mockito) : aucun email ne part, et chaque
 *   test peut le rendre lent ou bloquant.
 *
 * Tous les *IT partagent cette configuration et les memes proprietes
 * (IntegrationPostgres) : Spring garde un seul contexte, donc un seul
 * conteneur pour toute la suite.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfigurationPostgres {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }

    @Bean
    @Primary
    JavaMailSender serveurSmtpSimule() {
        return mock(JavaMailSender.class);
    }
}