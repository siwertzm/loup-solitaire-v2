package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "from", "no-reply@loup-solitaire.local");
    }

    @Test
    void envoyerEmailVerificationConstruitLeMessageAttendu() {
        emailService.envoyerEmailVerification(
                "marius@example.com",
                "http://localhost:8080/auth/verify-email?token=abc123"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@loup-solitaire.local");
        assertThat(message.getTo()).containsExactly("marius@example.com");
        assertThat(message.getSubject()).isEqualTo("Confirme ton compte Loup Solitaire");
        assertThat(message.getText()).contains("http://localhost:8080/auth/verify-email?token=abc123");
    }

    @Test
    void unEchecSmtpNePropagePasD_exception() {
        doThrow(new RuntimeException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        // La methode ne doit jamais faire planter l'appelant (ex. l'inscription)
        // a cause d'un souci d'envoi d'email : l'echec est journalise, pas propage.
        assertThatCode(() ->
                emailService.envoyerEmailVerification("marius@example.com", "http://localhost:8080/lien")
        ).doesNotThrowAnyException();
    }
}