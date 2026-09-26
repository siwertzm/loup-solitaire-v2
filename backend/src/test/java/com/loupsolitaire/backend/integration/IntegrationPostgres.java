package com.loupsolitaire.backend.integration;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * QUAL-01 : base des tests d'integration sur PostgreSQL (classes *IT,
 * lancees par "mvn verify" ; Docker doit tourner).
 *
 * Contrairement aux tests @SpringBootTest sur H2 :
 * - le schema est cree par les VRAIES migrations Liquibase (001 a 014),
 *   comme sur Render ;
 * - Hibernate verifie au demarrage que les entites correspondent a ce
 *   schema (ddl-auto=validate, comme en production) ;
 * - GameDataLoader charge le vrai catalogue du livre.
 *
 * Ne pas ajouter de @MockitoBean ni de proprietes dans une sous-classe :
 * Spring creerait un second contexte, donc un second conteneur.
 */
@SpringBootTest
@Import(ConfigurationPostgres.class)
@TestPropertySource(properties = {
        // Remplace par @ServiceConnection ; vide pour que ${DB_PASSWORD}
        // n'empeche pas le demarrage (SEC-03).
        "spring.datasource.password=",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.jpa.hibernate.ddl-auto=validate",
        // Les limites (SEC-02) sont testees a part ; ici, plusieurs tests
        // appellent /auth depuis la meme adresse.
        "app.limitation-debit.active=false"
})
public abstract class IntegrationPostgres {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected JavaMailSender serveurSmtp;

    @BeforeEach
    void reinitialiserLeServeurSmtp() {
        reset(serveurSmtp);
        when(serveurSmtp.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }
}