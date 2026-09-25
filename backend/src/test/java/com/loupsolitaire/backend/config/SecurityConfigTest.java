package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Regles de securite de l'API, avec la vraie chaine de filtres Spring
 * Security (SecurityConfig + JwtFilter + JwtAuthenticationEntryPoint) :
 * routes publiques ou protegees, 401 sans token valide, CORS.
 *
 * Memes proprietes que les autres tests @SpringBootTest : le contexte Spring
 * est reutilise d'un test a l'autre au lieu d'etre recree.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.datasource.hikari.connection-init-sql="
})
class SecurityConfigTest {

    // Origine autorisee par defaut (app.cors.allowed-origins).
    private static final String ORIGINE_AUTORISEE = "http://localhost:4200";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private String bearer() {
        return "Bearer " + jwtUtil.generateToken(UUID.randomUUID());
    }

    // =========================================================
    // Routes protegees
    // =========================================================

    @Test
    void uneRouteProtegeeSansTokenRepond401EnJson() throws Exception {
        mockMvc.perform(get("/personnages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Non authentifie"));
    }

    @Test
    void unTokenInvalideRepond401() throws Exception {
        mockMvc.perform(get("/personnages").header(HttpHeaders.AUTHORIZATION, "Bearer pas-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenSansPrefixeBearerRepond401() throws Exception {
        String tokenBrut = jwtUtil.generateToken(UUID.randomUUID());

        mockMvc.perform(get("/disciplines").header(HttpHeaders.AUTHORIZATION, tokenBrut))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenValideDonneAccesAuxDisciplines() throws Exception {
        // Les 10 disciplines sont chargees au demarrage par GameDataLoader.
        mockMvc.perform(get("/disciplines").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value("CAMOUFLAGE"));
    }

    @Test
    void unTokenValideDonneAccesAuxObjets() throws Exception {
        // Verifie aussi que les effets (LAZY) sont lus dans la transaction
        // du controleur, sans LazyInitializationException.
        mockMvc.perform(get("/objets").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(28));
    }

    // =========================================================
    // Routes publiques
    // =========================================================

    @Test
    void lesRoutesAuthSontPubliquesEtSansCsrf() throws Exception {
        // Corps invalide : 400 (validation), et non 401 (authentification)
        // ni 403 (CSRF desactive, l'API est sans session).
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leHealthcheckEstPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // =========================================================
    // CORS
    // =========================================================

    @Test
    void unPreflightDepuisUneOrigineAutoriseeEstAccepteSansCredentials() throws Exception {
        mockMvc.perform(options("/personnages")
                        .header(HttpHeaders.ORIGIN, ORIGINE_AUTORISEE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_AUTORISEE))
                // Aucun cookie : le token passe dans l'en-tete Authorization.
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void unPreflightDepuisUneOrigineInconnueEstRefuse() throws Exception {
        mockMvc.perform(options("/personnages")
                        .header(HttpHeaders.ORIGIN, "https://site-malveillant.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // =========================================================
    // Mots de passe
    // =========================================================

    @Test
    void lesMotsDePasseSontHachesAvecBcrypt() {
        String hash = passwordEncoder.encode("motdepasse123");

        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches("motdepasse123", hash)).isTrue();
        assertThat(passwordEncoder.matches("autre", hash)).isFalse();
    }
}