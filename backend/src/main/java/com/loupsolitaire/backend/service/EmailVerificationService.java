package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.EmailVerificationToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.EmailVerificationTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.util.Tokens;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;

    // Genere un token, le persiste (hash uniquement) et envoie l'email.
    // Utilise a l'inscription ET pour /auth/resend-verification.
    @Transactional
    public void envoyerLienDeVerification(Utilisateur utilisateur) {
        String rawToken = Tokens.genererAleatoire();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(Tokens.hacher(rawToken));
        token.setExpiresAt(Instant.now().plus(appProperties.emailVerification().expirationHours(), ChronoUnit.HOURS));
        tokenRepository.save(token);

        String lien = appProperties.baseUrl() + "/auth/verify-email?token=" + rawToken;
        emailService.envoyerEmailVerification(utilisateur.getEmail(), lien);
    }

    // Valide le token recu par email et active le compte correspondant.
    @Transactional
    public void verifier(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(Tokens.hacher(rawToken))
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

    // Suppression de compte (voir CompteService.supprimerCompte).
    @Transactional
    public void supprimerTokens(Utilisateur utilisateur) {
        tokenRepository.deleteByUtilisateur(utilisateur);
    }
}