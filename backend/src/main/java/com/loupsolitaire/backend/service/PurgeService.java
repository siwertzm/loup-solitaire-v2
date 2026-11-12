package com.loupsolitaire.backend.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.repository.EmailVerificationTokenRepository;
import com.loupsolitaire.backend.repository.PasswordResetTokenRepository;
import com.loupsolitaire.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OPS-05 : purge quotidienne des jetons qui ne servent plus. Sans elle,
 * chaque renouvellement de session (environ 4 par heure de jeu et par
 * joueur) ajoutait une ligne jamais supprimee.
 *
 * Les comptes ne sont jamais supprimes, meme non verifies (decision du
 * 12/11/2026) : seul leur lien expire disparait, "Renvoyer le lien" en
 * recree un.
 */
@Service
@RequiredArgsConstructor
@Slf4j
// Sur la classe, pas seulement sur purger() : la tache planifiee appelle
// purger() de l'interieur, et Spring n'applique @Transactional qu'aux appels
// venant de l'exterieur (les DELETE echoueraient alors sans transaction).
@Transactional
public class PurgeService {

    // Sessions expirees gardees encore 7 jours (traces recentes en cas
    // d'analyse d'un incident).
    static final Duration CONSERVATION_SESSIONS_EXPIREES = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    // Chaque jour a 4 h (heure de Paris), reglable par PURGE_CRON.
    @Scheduled(cron = "${app.purge.cron}", zone = "Europe/Paris")
    public void purgeQuotidienne() {
        purger(Instant.now());
    }

    public Resultat purger(Instant maintenant) {
        Resultat resultat = new Resultat(
                refreshTokenRepository.supprimerExpiresAvant(maintenant.minus(CONSERVATION_SESSIONS_EXPIREES)),
                emailVerificationTokenRepository.supprimerExpires(maintenant),
                passwordResetTokenRepository.supprimerExpires(maintenant));
        log.info("Purge : {} sessions, {} liens de verification, {} demandes de mot de passe supprimes",
                resultat.sessions(), resultat.liensDeVerification(), resultat.demandesMotDePasse());
        return resultat;
    }

    public record Resultat(int sessions, int liensDeVerification, int demandesMotDePasse) {
    }
}
