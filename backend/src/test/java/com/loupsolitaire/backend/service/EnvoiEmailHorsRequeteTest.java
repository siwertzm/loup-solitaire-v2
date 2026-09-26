package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.config.EnvoiEmailsConfig;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * OPS-02 avec les vraies transactions Spring et la vraie file d'envoi
 * (EnvoiEmailsConfig) ; seul le serveur SMTP est simule :
 * - un SMTP lent ne ralentit pas la requete ;
 * - une transaction annulee n'envoie aucun email.
 *
 * Memes proprietes que les autres tests @SpringBootTest (et pas de
 * @MockitoBean) : le contexte Spring deja demarre est reutilise, aucun second
 * contexte n'est cree. L'EmailService teste est construit a la main avec un
 * JavaMailSender simule.
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
class EnvoiEmailHorsRequeteTest {

    private static final String LIEN = "http://localhost:8080/auth/verify-email?token=abc";

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    @Qualifier(EnvoiEmailsConfig.EXECUTOR)
    private Executor envoiEmails;

    private JavaMailSender mailSender;
    private EmailService emailService;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        emailService = new EmailService(mailSender, appProperties, envoiEmails);
        transaction = new TransactionTemplate(transactionManager);
    }

    @Test
    void unSmtpLentNeRalentitPasLaRequete() {
        // Le serveur SMTP met 3 s a repondre.
        doAnswer(invocation -> {
            Thread.sleep(3_000);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        Instant debut = Instant.now();
        // Comme une requete : transaction ouverte, email demande, commit.
        transaction.executeWithoutResult(status ->
                emailService.envoyerEmailVerification("marius@example.com", LIEN));
        Duration duree = Duration.between(debut, Instant.now());

        assertThat(duree).isLessThan(Duration.ofSeconds(1));
        // L'email part quand meme, depuis la file d'envoi.
        verify(mailSender, timeout(5_000)).send(any(MimeMessage.class));
    }

    @Test
    void uneTransactionAnnuleeNEnvoieAucunEmail() {
        transaction.executeWithoutResult(status -> {
            emailService.envoyerCodeReinitialisationMotDePasse("marius@example.com", "123456", 15);
            status.setRollbackOnly();
        });

        verify(mailSender, after(1_000).never()).send(any(MimeMessage.class));
    }
}