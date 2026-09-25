package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.JwtProperties;
import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.RefreshToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.RefreshTokenRepository;
import com.loupsolitaire.backend.util.Tokens;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    // Cree un nouveau refresh token pour l'utilisateur et renvoie la valeur BRUTE
    // (a transmettre au client). Seul le hash est persiste en base.
    @Transactional
    public String creerToken(Utilisateur utilisateur) {
        String rawToken = Tokens.genererAleatoire();

        RefreshToken token = new RefreshToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(Tokens.hacher(rawToken));
        token.setExpiresAt(Instant.now().plus(jwtProperties.refreshExpirationDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    // Valide le refresh token presente, le fait pivoter (revoque l'ancien, en
    // cree un nouveau) et renvoie l'utilisateur + le nouveau token brut.
    // Si un token DEJA REVOQUE est presente, c'est le signe d'un vol/rejeu :
    // toutes les sessions actives de l'utilisateur sont revoquees par securite.
    @Transactional
    public RotationResult validerEtPivoter(String rawToken) {
        String hash = Tokens.hacher(rawToken);
        RefreshToken existant = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenInvalideException("Session invalide, merci de vous reconnecter"));

        if (existant.isRevoked()) {
            revoquerToutesLesSessions(existant.getUtilisateur());
            throw new TokenInvalideException(
                    "Reutilisation d'un token deja utilise detectee : toutes les sessions ont ete revoquees par securite"
            );
        }

        if (existant.isExpired()) {
            throw new TokenInvalideException("Session expiree, merci de vous reconnecter");
        }

        existant.setRevoked(true);
        refreshTokenRepository.save(existant);

        // On ne renvoie que l'identifiant de l'utilisateur, pas l'entite
        // elle-meme : son champ 'utilisateur' sur RefreshToken est charge en
        // LAZY, et le lire hors de cette transaction (ex. dans le controleur)
        // leverait LazyInitializationException. L'identifiant suffit pour
        // generer le nouvel access token (voir JwtUtil).
        Utilisateur utilisateur = existant.getUtilisateur();

        String nouveauToken = creerToken(utilisateur);
        return new RotationResult(utilisateur.getId(), nouveauToken);
    }

    @Transactional
    public void revoquer(String rawToken) {
        refreshTokenRepository.findByTokenHash(Tokens.hacher(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revoquerToutesLesSessions(Utilisateur utilisateur) {
        refreshTokenRepository.findAllByUtilisateurAndRevokedFalse(utilisateur)
                .forEach(token -> token.setRevoked(true));
    }

    // Suppression de compte : les sessions sont supprimees (pas seulement
    // revoquees), sinon la cle etrangere utilisateur_id bloquerait la
    // suppression de l'utilisateur.
    @Transactional
    public void supprimerToutesLesSessions(Utilisateur utilisateur) {
        refreshTokenRepository.deleteByUtilisateur(utilisateur);
    }

    public record RotationResult(UUID utilisateurId, String nouveauRefreshToken) {
    }
}