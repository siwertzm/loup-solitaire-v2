package com.loupsolitaire.backend.config;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Genere et lit les access tokens JWT.
 *
 * Le "subject" du token est l'IDENTIFIANT (UUID) de l'utilisateur, et non son
 * nom : le nom d'utilisateur est modifiable (PUT /auth/me). Avec un nom dans
 * le token, un token emis pour "bob" restait valide apres que Bob se soit
 * renomme, et authentifiait alors quiconque reprenait ensuite le nom "bob".
 * L'UUID, lui, ne change jamais et n'est jamais reattribue.
 */
@Component
public class JwtUtil {

    // Calculee une seule fois au demarrage (avant : decodee a chaque requete),
    // a partir de jwt.secret (variable d'environnement JWT_SECRET, verifiee
    // dans JwtProperties).
    private final SecretKey cleDeSignature;
    private final long expirationMs;

    public JwtUtil(JwtProperties proprietes) {
        this.cleDeSignature = proprietes.cleDeSignature();
        this.expirationMs = proprietes.expirationMs();
    }

    public String generateToken(UUID utilisateurId) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(utilisateurId.toString())
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + expirationMs))
                .signWith(cleDeSignature, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifie la signature et l'expiration du token, puis renvoie l'identifiant
     * de l'utilisateur qu'il designe.
     *
     * @throws JwtException si le token est expire, mal signe ou illisible
     *         (ExpiredJwtException, SignatureException, MalformedJwtException...)
     * @throws IllegalArgumentException si le subject n'est pas un UUID (par
     *         exemple un ancien token emis avant ce changement, qui contenait
     *         un nom d'utilisateur : il est simplement refuse, et le client
     *         en obtient un nouveau via /auth/refresh)
     */
    public UUID extractUtilisateurId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(cleDeSignature)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}