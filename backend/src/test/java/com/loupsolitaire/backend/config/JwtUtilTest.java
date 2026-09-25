package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// Cle de test generee arbitrairement, jamais utilisee en dehors de ce test.
class JwtUtilTest {

    private static final String SECRET = "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi";
    private static final String AUTRE_SECRET = "YXV0cmVfY2xlX3NlY3JldGVfcG91cl9sZXNfdGVzdHNfdW5pdGFpcmVz";

    private JwtUtil jwtUtil;
    private final UUID utilisateurId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(ProprietesDeTest.jwt(SECRET, 3_600_000L));
    }

    @Test
    void genereUnTokenContenantLIdentifiantDeLUtilisateur() {
        String token = jwtUtil.generateToken(utilisateurId);

        assertThat(jwtUtil.extractUtilisateurId(token)).isEqualTo(utilisateurId);
    }

    @Test
    void unTokenDejaExpireEstRefuse() {
        // Expiration negative : le token est genere deja perime.
        jwtUtil = new JwtUtil(ProprietesDeTest.jwt(SECRET, -1_000L));
        String token = jwtUtil.generateToken(utilisateurId);

        assertThatThrownBy(() -> jwtUtil.extractUtilisateurId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void unTokenSigneAvecUneAutreCleEstRefuse() {
        JwtUtil autreServeur = new JwtUtil(ProprietesDeTest.jwt(AUTRE_SECRET, 3_600_000L));
        String tokenEtranger = autreServeur.generateToken(utilisateurId);

        assertThatThrownBy(() -> jwtUtil.extractUtilisateurId(tokenEtranger))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenIllisibleEstRefuse() {
        assertThatThrownBy(() -> jwtUtil.extractUtilisateurId("pas-un-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unAncienTokenContenantUnNomDUtilisateurEstRefuse() {
        // Token au format d'avant la correction : bien signe, non expire,
        // mais avec un nom d'utilisateur comme subject au lieu d'un UUID.
        String ancienToken = Jwts.builder()
                .subject("marius")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtUtil.extractUtilisateurId(ancienToken))
                .isInstanceOf(IllegalArgumentException.class);
    }
}