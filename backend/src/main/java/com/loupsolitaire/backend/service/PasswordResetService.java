package com.loupsolitaire.backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.PasswordResetToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PasswordResetTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.password-reset.expiration-minutes:15}")
    private long expirationMinutes;

    @Value("${app.password-reset.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void demanderReinitialisation(String email) {

        /*
         * IMPORTANT :
         * Si l'adresse n'existe pas, on ne lève aucune erreur.
         *
         * Le contrôleur renverra donc exactement la même réponse,
         * que le compte existe ou non.
         *
         * Cela évite de permettre à quelqu'un de tester quels emails
         * sont inscrits dans l'application.
         */
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {

            /*
             * Les anciens codes encore valides deviennent inutilisables
             * dès qu'un nouveau code est demandé.
             */
            tokenRepository.findByUtilisateurAndUtiliseFalse(utilisateur)
                    .forEach(token -> token.setUtilise(true));

            String code = genererCode();

            PasswordResetToken token = new PasswordResetToken();
            token.setUtilisateur(utilisateur);

            /*
             * BCrypt plutôt que SHA-256 :
             * un code à 6 chiffres possède très peu de combinaisons,
             * donc il ne faut pas stocker un simple hash rapide.
             */
            token.setCodeHash(passwordEncoder.encode(code));

            token.setExpiresAt(
                    Instant.now().plus(
                            expirationMinutes,
                            ChronoUnit.MINUTES
                    )
            );

            tokenRepository.save(token);

            emailService.envoyerCodeReinitialisationMotDePasse(
                    utilisateur.getEmail(),
                    code,
                    expirationMinutes
            );
        });
    }

    @Transactional
    public void reinitialiser(
            String email,
            String code,
            String nouveauMotDePasse
    ) {

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Code invalide ou expire"
                        )
                );

        PasswordResetToken token =
                tokenRepository
                        .findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(
                                utilisateur
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Code invalide ou expire"
                                )
                        );

        if (token.isExpired()) {
            token.setUtilise(true);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        if (token.getTentatives() >= maxAttempts) {
            token.setUtilise(true);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {

            int nouvellesTentatives = token.getTentatives() + 1;

            token.setTentatives(nouvellesTentatives);

            if (nouvellesTentatives >= maxAttempts) {
                token.setUtilise(true);
            }

            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        /*
         * Le code est correct.
         */
        utilisateur.setPassword(
                passwordEncoder.encode(nouveauMotDePasse)
        );

        utilisateurRepository.save(utilisateur);

        /*
         * Le code ne peut plus être réutilisé.
         */
        token.setUtilise(true);
        tokenRepository.save(token);

        /*
         * Déconnexion de tous les appareils après changement
         * du mot de passe.
         */
        refreshTokenService.revoquerToutesLesSessions(utilisateur);
    }

    private String genererCode() {

        int valeur = SECURE_RANDOM.nextInt(1_000_000);

        return String.format("%06d", valeur);
    }
}