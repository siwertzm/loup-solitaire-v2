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

    public void envoyerEmailVerification(
            String destinataire,
            String lienConfirmation
    ) {

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(destinataire);

            message.setSubject(
                    "Confirme ton compte Loup Solitaire"
            );

            message.setText(
                    "Bienvenue dans l'aventure !\n\n" +
                    "Confirme ton adresse email en cliquant sur ce lien " +
                    "(valable 24h) :\n" +
                    lienConfirmation + "\n\n" +
                    "Si tu n'es pas a l'origine de cette inscription, " +
                    "ignore cet email."
            );

            mailSender.send(message);

        } catch (Exception e) {

            log.error(
                    "Echec de l'envoi de l'email de verification a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }

    public void envoyerCodeReinitialisationMotDePasse(
            String destinataire,
            String code,
            long expirationMinutes
    ) {

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(destinataire);

            message.setSubject(
                    "Reinitialisation de ton mot de passe Loup Solitaire"
            );

            message.setText(
                    "Une demande de reinitialisation de ton mot de passe " +
                    "a ete effectuee.\n\n" +

                    "Voici ton code :\n\n" +

                    code + "\n\n" +

                    "Ce code est valable pendant " +
                    expirationMinutes +
                    " minutes.\n\n" +

                    "Si tu n'es pas a l'origine de cette demande, " +
                    "tu peux ignorer cet email."
            );

            mailSender.send(message);

        } catch (Exception e) {

            log.error(
                    "Echec de l'envoi de l'email de reinitialisation a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }
}