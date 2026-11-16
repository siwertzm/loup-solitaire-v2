package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
    private final LimiteurDeDebit limiteurDeDebit;

    // SEC-11 : hachage d'un code au hasard, compare quand il n'y a pas de
    // vrai code (voir verifierCodeSansLimite). Calcule au premier besoin ;
    // deux calculs simultanes donnent deux valeurs aussi valables.
    private volatile String hashFactice;

    /*
     * ETAPE 1
     *
     * L'utilisateur saisit son email.
     * On génère et envoie un code à 6 chiffres.
     */
    @Transactional
    public void demanderReinitialisation(String email) {

        /*
         * SEC-02 : quelques codes par heure et par email, que l'adresse
         * existe ou non. Sans cette limite, chaque nouvelle demande donnait
         * un nouveau code avec un compteur d'essais remis a zero : essais
         * illimites sur un code a 6 chiffres.
         */
        limiteurDeDebit.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, email);

        /*
         * Ne jamais révéler si l'adresse existe ou non.
         */
        Optional<Utilisateur> compte = utilisateurRepository.findByEmail(email);
        if (compte.isEmpty()) {
            // SEC-11 : meme hachage bcrypt que pour un vrai compte, pour que
            // la duree de la reponse ne dise pas si l'email existe.
            passwordEncoder.encode(Tokens.genererCodeASixChiffres());
            return;
        }
        compte.ifPresent(utilisateur -> {

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

        /*
         * SEC-02 : un nombre limite de codes faux par email et par jour, tous
         * codes confondus (le compteur de tentatives du token, lui, repart a
         * zero a chaque nouveau code). Un echec compte aussi pour un email
         * inconnu : la reponse reste identique.
         */
        limiteurDeDebit.verifier(LimiteurDeDebit.RESET_CODE_EMAIL, email);

        try {
            String resetToken = verifierCodeSansLimite(email, code);
            limiteurDeDebit.reinitialiser(LimiteurDeDebit.RESET_CODE_EMAIL, email);
            return resetToken;
        } catch (IllegalArgumentException e) {
            limiteurDeDebit.enregistrerEchec(LimiteurDeDebit.RESET_CODE_EMAIL, email);
            throw e;
        }
    }

    private String verifierCodeSansLimite(
            String email,
            String code
    ) {

        PasswordResetToken token = utilisateurRepository.findByEmail(email)
                .flatMap(tokenRepository::findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc)
                .orElse(null);

        /*
         * SEC-11 : une seule comparaison bcrypt, faite dans TOUS les cas
         * (email inconnu, pas de code en cours, code deja valide, expire,
         * trop d'essais) : sans ca, la duree de la reponse disait si l'email
         * a un compte.
         */
        boolean codeCorrect = passwordEncoder.matches(
                code,
                token != null ? token.getCodeHash() : hashFactice()
        );

        if (token == null || token.getResetTokenHash() != null) {
            throw new IllegalArgumentException("Code invalide ou expire");
        }

        if (token.isExpired() || token.getTentatives() >= maxAttempts()) {
            token.setUtilise(true);
            tokenRepository.save(token);
            throw new IllegalArgumentException("Code invalide ou expire");
        }

        /*
         * Mauvais code.
         */
        if (!codeCorrect) {
            int nouvellesTentatives = token.getTentatives() + 1;
            token.setTentatives(nouvellesTentatives);
            if (nouvellesTentatives >= maxAttempts()) {
                token.setUtilise(true);
            }
            tokenRepository.save(token);
            throw new IllegalArgumentException("Code invalide ou expire");
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

    private String hashFactice() {
        if (hashFactice == null) {
            hashFactice = passwordEncoder.encode(Tokens.genererCodeASixChiffres());
        }
        return hashFactice;
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