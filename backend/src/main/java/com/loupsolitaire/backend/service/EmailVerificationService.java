package com.loupsolitaire.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.EmailVerificationToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.EmailVerificationTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.expiration-hours}")
    private long expirationHours;

    @Value("${app.base-url}")
    private String baseUrl;

    // Genere un token, le persiste (hash uniquement) et envoie l'email.
    // Utilise a l'inscription ET pour /auth/resend-verification.
    @Transactional
    public void envoyerLienDeVerification(Utilisateur utilisateur) {
        String rawToken = genererValeurAleatoire();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(hacher(rawToken));
        token.setExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS));
        tokenRepository.save(token);

        String lien = baseUrl + "/auth/verify-email?token=" + rawToken;
        emailService.envoyerEmailVerification(utilisateur.getEmail(), lien);
    }

    // Valide le token recu par email et active le compte correspondant.
    @Transactional
    public void verifier(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hacher(rawToken))
                .orElseThrow(() -> new TokenInvalideException("Lien de confirmation invalide"));

        if (token.isUtilise()) {
            throw new TokenInvalideException("Ce lien de confirmation a deja ete utilise");
        }
        if (token.isExpired()) {
            throw new TokenInvalideException("Ce lien de confirmation a expire, demande un nouvel envoi");
        }

        Utilisateur utilisateur = token.getUtilisateur();
        utilisateur.setEmailVerifie(true);
        utilisateurRepository.save(utilisateur);

        token.setUtilise(true);
        tokenRepository.save(token);
    }

    private String genererValeurAleatoire() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hacher(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}