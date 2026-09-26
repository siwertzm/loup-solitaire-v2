package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.loupsolitaire.backend.config.ProprietesDeTest;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * OPS-02 : l'email part APRES la validation de la transaction, depuis la
 * file d'envoi, jamais dans le fil de la requete.
 *
 * La transaction est simulee avec TransactionSynchronizationManager (ce que
 * fait Spring autour d'une methode @Transactional) ; la file d'envoi est une
 * simple liste, pour controler quand l'envoi s'execute.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceApresCommitTest {

    private static final String LIEN = "http://localhost:8080/auth/verify-email?token=abc";

    @Mock
    private JavaMailSender mailSender;

    private final List<Runnable> file = new ArrayList<>();

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, ProprietesDeTest.app(), file::add);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rienNePartAvantLeCommitPuisLEmailPartDepuisLaFile() {
        TransactionSynchronizationManager.initSynchronization();

        emailService.envoyerEmailVerification("marius@example.com", LIEN);

        // Pendant la transaction : rien en file, rien envoye.
        assertThat(file).isEmpty();
        verify(mailSender, never()).send(any(MimeMessage.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // Apres le commit : mis en file, pas encore envoye par le fil appelant.
        assertThat(file).hasSize(1);
        verify(mailSender, never()).send(any(MimeMessage.class));

        file.get(0).run();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void rienNePartSiLaTransactionEstAnnulee() {
        TransactionSynchronizationManager.initSynchronization();

        emailService.envoyerCodeReinitialisationMotDePasse("marius@example.com", "123456", 15);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(file).isEmpty();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sansTransactionLEmailEstMisEnFileDirectement() {
        emailService.envoyerEmailVerification("marius@example.com", LIEN);

        assertThat(file).hasSize(1);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void uneFilePleineNeFaitPasEchouerLaRequete() {
        EmailService fileSaturee = new EmailService(mailSender, ProprietesDeTest.app(), envoi -> {
            throw new RejectedExecutionException("file pleine");
        });

        assertThatCode(() -> fileSaturee.envoyerEmailVerification("marius@example.com", LIEN))
                .doesNotThrowAnyException();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void unEchecSmtpDansLaFileEstJournaliseSansException() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(MimeMessage.class));

        emailService.envoyerEmailVerification("marius@example.com", LIEN);

        assertThatCode(() -> file.get(0).run()).doesNotThrowAnyException();
    }
}