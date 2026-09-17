package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                emailService,
                "from",
                "no-reply@loup-solitaire.local"
        );

        mimeMessage = new MimeMessage(
                Session.getInstance(
                        new Properties()
                )
        );

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);
    }

    @Test
    void envoyerEmailVerificationConstruitLeMessageAttendu()
            throws Exception {

        String lien =
                "http://localhost:8080/auth/verify-email?token=abc123";

        emailService.envoyerEmailVerification(
                "marius@example.com",
                lien
        );

        verify(mailSender)
                .send(mimeMessage);

        /*
         * Finalise les headers MIME pour pouvoir
         * les inspecter proprement.
         */
        mimeMessage.saveChanges();

        assertThat(
                mimeMessage.getFrom()[0].toString()
        ).isEqualTo(
                "no-reply@loup-solitaire.local"
        );

        assertThat(
                mimeMessage.getRecipients(
                        Message.RecipientType.TO
                )[0].toString()
        ).isEqualTo(
                "marius@example.com"
        );

        assertThat(
                mimeMessage.getSubject()
        ).isEqualTo(
                "Confirme ton entrée dans l'aventure — Loup Solitaire"
        );

        String contenu =
                extraireTexte(mimeMessage);

        assertThat(contenu)
                .contains(lien);

        assertThat(contenu)
                .contains("Loup Solitaire");
    }

    @Test
    void unEchecSmtpNePropagePasD_exception() {

        doThrow(
                new RuntimeException(
                        "SMTP indisponible"
                )
        )
        .when(mailSender)
        .send(
                any(MimeMessage.class)
        );

        /*
         * Un problème SMTP ne doit pas faire
         * échouer l'inscription.
         */
        assertThatCode(
                () ->
                        emailService
                                .envoyerEmailVerification(
                                        "marius@example.com",
                                        "http://localhost:8080/lien"
                                )
        )
        .doesNotThrowAnyException();

        verify(mailSender)
                .send(
                        any(MimeMessage.class)
                );
    }

    /**
     * Parcourt récursivement les parties MIME
     * text/plain et text/html.
     */
    private String extraireTexte(
            Part part
    ) throws Exception {

        if (part.isMimeType("text/*")) {

            Object contenu =
                    part.getContent();

            return contenu != null
                    ? contenu.toString()
                    : "";
        }

        if (part.isMimeType("multipart/*")) {

            Multipart multipart =
                    (Multipart) part.getContent();

            StringBuilder resultat =
                    new StringBuilder();

            for (
                    int i = 0;
                    i < multipart.getCount();
                    i++
            ) {

                resultat.append(
                        extraireTexte(
                                multipart.getBodyPart(i)
                        )
                );

                resultat.append('\n');
            }

            return resultat.toString();
        }

        return "";
    }
}