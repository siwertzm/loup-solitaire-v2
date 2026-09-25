package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.model.PasswordResetToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PasswordResetTokenRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.util.Tokens;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;

    /*
     * ETAPE 1
     *
     * L'utilisateur saisit son email.
     * On génère et envoie un code à 6 chiffres.
     */
    @Transactional
    public void demanderReinitialisation(String email) {

        /*
         * Ne jamais révéler si l'adresse existe ou non.
         */
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {

            /*
             * Un nouveau code invalide tous les précédents.
             */
            tokenRepository.findByUtilisateurAndUtiliseFalse(utilisateur)
                    .forEach(token -> token.setUtilise(true));

            String code = Tokens.genererCodeASixChiffres();

            PasswordResetToken token = new PasswordResetToken();

            token.setUtilisateur(utilisateur);
            token.setCodeHash(passwordEncoder.encode(code));

            token.setExpiresAt(
                    Instant.now().plus(
                            expirationMinutes(),
                            ChronoUnit.MINUTES
                    )
            );

            tokenRepository.save(token);

            emailService.envoyerCodeReinitialisationMotDePasse(
                    utilisateur.getEmail(),
                    code,
                    expirationMinutes()
            );
        });
    }

    /*
     * ETAPE 2
     *
     * L'utilisateur saisit le code reçu par email.
     *
     * Si le code est bon, on génère un nouveau token aléatoire,
     * utilisable uniquement pour l'étape "nouveau mot de passe".
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verifierCode(
            String email,
            String code
    ) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Code invalide ou expire"
                        )
                );

        PasswordResetToken token = tokenRepository
                .findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(
                        utilisateur
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Code invalide ou expire"
                        )
                );

        /*
         * Le code a déjà été validé.
         *
         * resetTokenHash != null signifie que l'étape 2
         * a déjà été franchie.
         */
        if (token.getResetTokenHash() != null) {
            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        if (token.isExpired()) {

            token.setUtilise(true);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        if (token.getTentatives() >= maxAttempts()) {

            token.setUtilise(true);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        /*
         * Mauvais code.
         */
        if (!passwordEncoder.matches(
                code,
                token.getCodeHash()
        )) {

            int nouvellesTentatives =
                    token.getTentatives() + 1;

            token.setTentatives(nouvellesTentatives);

            if (nouvellesTentatives >= maxAttempts()) {
                token.setUtilise(true);
            }

            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Code invalide ou expire"
            );
        }

        /*
         * Le code est valide.
         *
         * Création d'un token de réinitialisation
         * aléatoire et non prédictible.
         */
        String resetToken = Tokens.genererAleatoire();

        token.setResetTokenHash(
                Tokens.hacher(resetToken)
        );

        /*
         * On utilise ici la même durée que celle du code.
         *
         * Avec ta configuration actuelle :
         * 15 minutes.
         */
        token.setResetTokenExpiresAt(
                Instant.now().plus(
                        expirationMinutes(),
                        ChronoUnit.MINUTES
                )
        );

        tokenRepository.save(token);

        /*
         * Seule la valeur brute est retournée au frontend.
         * La base ne possède que son SHA-256.
         */
        return resetToken;
    }

    /*
     * ETAPE 3
     *
     * L'utilisateur choisit ses nouveaux mots de passe.
     *
     * Le frontend envoie uniquement :
     *
     * - resetToken
     * - newPassword
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void reinitialiser(
            String resetToken,
            String nouveauMotDePasse
    ) {

        PasswordResetToken token = tokenRepository
                .findByResetTokenHashAndUtiliseFalse(
                        Tokens.hacher(resetToken)
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Reinitialisation invalide ou expiree"
                        )
                );

        if (token.getResetTokenHash() == null
                || token.isResetTokenExpired()) {

            token.setUtilise(true);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Reinitialisation invalide ou expiree"
            );
        }

        Utilisateur utilisateur =
                token.getUtilisateur();

        /*
         * Modification du mot de passe.
         */
        utilisateur.setPassword(
                passwordEncoder.encode(
                        nouveauMotDePasse
                )
        );

        utilisateurRepository.save(utilisateur);

        /*
         * Invalidation de tous les codes / tokens de
         * réinitialisation encore ouverts.
         */
        tokenRepository
                .findByUtilisateurAndUtiliseFalse(utilisateur)
                .forEach(t -> t.setUtilise(true));

        /*
         * Toutes les anciennes sessions sont déconnectées.
         */
        refreshTokenService
                .revoquerToutesLesSessions(utilisateur);
    }

    private long expirationMinutes() {
        return appProperties.passwordReset().expirationMinutes();
    }

    private int maxAttempts() {
        return appProperties.passwordReset().maxAttempts();
    }

    // Suppression de compte (voir CompteService.supprimerCompte).
    @Transactional
    public void supprimerTokens(Utilisateur utilisateur) {
        tokenRepository.deleteByUtilisateur(utilisateur);
    }
}