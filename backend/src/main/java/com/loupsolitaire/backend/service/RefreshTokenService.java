package com.loupsolitaire.backend.service;

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
import com.loupsolitaire.backend.model.RefreshToken;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    // Cree un nouveau refresh token pour l'utilisateur et renvoie la valeur BRUTE
    // (a transmettre au client). Seul le hash est persiste en base.
    @Transactional
    public String creerToken(Utilisateur utilisateur) {
        String rawToken = genererValeurAleatoire();

        RefreshToken token = new RefreshToken();
        token.setUtilisateur(utilisateur);
        token.setTokenHash(hacher(rawToken));
        token.setExpiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    // Valide le refresh token presente, le fait pivoter (revoque l'ancien, en
    // cree un nouveau) et renvoie l'utilisateur + le nouveau token brut.
    // Si un token DEJA REVOQUE est presente, c'est le signe d'un vol/rejeu :
    // toutes les sessions actives de l'utilisateur sont revoquees par securite.
    @Transactional
    public RotationResult validerEtPivoter(String rawToken) {
        String hash = hacher(rawToken);
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

        String nouveauToken = creerToken(existant.getUtilisateur());
        return new RotationResult(existant.getUtilisateur(), nouveauToken);
    }

    @Transactional
    public void revoquer(String rawToken) {
        refreshTokenRepository.findByTokenHash(hacher(rawToken))
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

    private String genererValeurAleatoire() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hacher(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valeur.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    public record RotationResult(Utilisateur utilisateur, String nouveauRefreshToken) {
    }
}
