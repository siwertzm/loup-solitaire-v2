package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

// Cle de test generee arbitrairement, jamais utilisee en dehors de ce test.
class JwtUtilTest {

    private static final String SECRET = "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi";

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // secretKey et expirationMs sont injectes par @Value en production ;
        // on les fixe manuellement ici puisqu'il n'y a pas de contexte Spring.
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);

        userDetails = User.builder()
                .username("marius")
                .password("hash-peu-importe")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void genereUnTokenContenantLeBonUsername() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("marius");
    }

    @Test
    void unTokenFraichementGenereEstValide() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void unTokenPourUnAutreUtilisateurEstInvalide() {
        String token = jwtUtil.generateToken(userDetails);
        UserDetails autreUtilisateur = User.builder()
                .username("quelqu-un-d-autre")
                .password("peu-importe")
                .authorities("ROLE_USER")
                .build();

        assertThat(jwtUtil.isTokenValid(token, autreUtilisateur)).isFalse();
    }

    @Test
    void unTokenDejaExpireEstInvalide() {
        // Expiration negative : le token est genere deja perime.
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1_000L);
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, userDetails)).isFalse();
    }
}