package com.loupsolitaire.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    // N'echoue jamais bruyamment : un souci SMTP ne doit pas empecher
    // l'inscription elle-meme. On journalise et on laisse l'appelant proposer
    // un renvoi via /auth/resend-verification si besoin.
    public void envoyerEmailVerification(String destinataire, String lienConfirmation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(destinataire);
            message.setSubject("Confirme ton compte Loup Solitaire");
            message.setText(
                "Bienvenue dans l'aventure !\n\n" +
                "Confirme ton adresse email en cliquant sur ce lien (valable 24h) :\n" +
                lienConfirmation + "\n\n" +
                "Si tu n'es pas a l'origine de cette inscription, ignore cet email."
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Echec de l'envoi de l'email de verification a {} : {}", destinataire, e.getMessage());
        }
    }
}