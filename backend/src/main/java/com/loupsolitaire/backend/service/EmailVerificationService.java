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
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.util.Emails;
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
    // Utilise a l'inscription ET pour /auth/resend-verification. SEC-06 : si
    // un changement d'email est en attente, le lien part a la nouvelle adresse.
    @Transactional
    public void envoyerLienDeVerification(Utilisateur utilisateur) {
        String lien = creerLien(utilisateur);
        if (utilisateur.getEmailEnAttente() != null) {
            emailService.envoyerConfirmationNouvelEmail(utilisateur.getEmailEnAttente(), lien);
        } else {
            emailService.envoyerEmailVerification(utilisateur.getEmail(), lien);
        }
    }

    // SEC-06 : le nouvel email est mis en attente ; l'email du compte ne
    // change qu'au clic sur le lien. Les liens deja envoyes sont annules :
    // un lien parti a une adresse mal tapee ne peut plus rien confirmer.
    // L'ancienne adresse est prevenue.
    @Transactional
    public void demanderChangementEmail(Utilisateur utilisateur, String nouvelEmail) {
        tokenRepository.deleteByUtilisateur(utilisateur);
        utilisateur.setEmailEnAttente(nouvelEmail);
        envoyerLienDeVerification(utilisateur);
        emailService.envoyerAlerteChangementEmail(utilisateur.getEmail(), Emails.masquer(utilisateur.getEmailEnAttente()));
    }

    private String creerLien(Utilisateur utilisateur) {
        String rawToken = Tokens.genererAleatoire();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(Tokens.hacher(rawToken));
        token.setExpiresAt(Instant.now().plus(appProperties.emailVerification().expirationHours(), ChronoUnit.HOURS));
        tokenRepository.save(token);

        return appProperties.baseUrl() + "/auth/verify-email?token=" + rawToken;
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
        // SEC-06 : confirmation d'un changement d'email. L'adresse a pu etre
        // prise par un autre compte depuis la demande.
        String enAttente = utilisateur.getEmailEnAttente();
        if (enAttente != null) {
            if (utilisateurRepository.existsByEmail(enAttente)) {
                throw new ConflitException("Cette adresse est deja utilisee par un autre compte");
            }
            utilisateur.setEmail(enAttente);
            utilisateur.setEmailEnAttente(null);
        }
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