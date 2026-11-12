package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.mail.internet.MimeMessage;

import com.jayway.jsonpath.JsonPath;

/**
 * SEC-06, de bout en bout sur PostgreSQL : changer d'email exige le mot de
 * passe ; tant que le lien n'est pas confirme, l'ancien email reste celui du
 * compte ; apres confirmation, seul le nouveau permet de se connecter.
 */
class ChangementEmailIT extends IntegrationPostgres {

    private static final String MOT_DE_PASSE = "motdepasse-sec06";
    private static final Pattern LIEN = Pattern.compile("/auth/verify-email\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final String suffixe = UUID.randomUUID().toString().substring(0, 8);
    private final String ancien = "ancien-" + suffixe + "@test.local";
    private final String nouveau = "nouveau-" + suffixe + "@test.local";

    @BeforeEach
    void unJoueurVerifie() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "sec06-%s", "email": "%s", "password": "%s"}
                        """.formatted(suffixe, ancien, MOT_DE_PASSE)))
                .andExpect(status().isAccepted());
        jdbc.update("update utilisateur set email_verifie = true where email = ?", ancien);
    }

    @AfterEach
    void nettoyer() {
        String condition = "(select id from utilisateur where username = ?)";
        jdbc.update("delete from email_verification_token where utilisateur_id in " + condition, "sec06-" + suffixe);
        jdbc.update("delete from refresh_token where utilisateur_id in " + condition, "sec06-" + suffixe);
        jdbc.update("delete from utilisateur where username = ?", "sec06-" + suffixe);
    }

    private int connexion(String identifiant) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identifiant": "%s", "password": "%s"}
                        """.formatted(identifiant, MOT_DE_PASSE)))
                .andReturn().getResponse().getStatus();
    }

    private String jeton() throws Exception {
        String reponse = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identifiant": "%s", "password": "%s"}
                        """.formatted(ancien, MOT_DE_PASSE)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(reponse, "$.accessToken");
    }

    // Lien du dernier email envoye a cette adresse (serveur SMTP simule).
    private String lienRecuPar(String destinataire) throws Exception {
        ArgumentCaptor<MimeMessage> envoyes = ArgumentCaptor.forClass(MimeMessage.class);
        verify(serveurSmtp, timeout(5000).atLeastOnce()).send(envoyes.capture());
        List<MimeMessage> messages = envoyes.getAllValues();
        for (int i = messages.size() - 1; i >= 0; i--) {
            MimeMessage message = messages.get(i);
            if (message.getAllRecipients()[0].toString().equals(destinataire)) {
                message.saveChanges();
                java.io.ByteArrayOutputStream contenu = new java.io.ByteArrayOutputStream();
                message.writeTo(contenu);
                Matcher lien = LIEN.matcher(contenu.toString().replace("=\r\n", "").replace("=3D", "="));
                if (lien.find()) {
                    return lien.group(1);
                }
            }
        }
        throw new AssertionError("Aucun lien envoye a " + destinataire);
    }

    @Test
    void changerDEmailSansLeBonMotDePasseRepond401() throws Exception {
        String jeton = jeton();

        mockMvc.perform(put("/auth/me").header(HttpHeaders.AUTHORIZATION, jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\"}".formatted(nouveau)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/auth/me").header(HttpHeaders.AUTHORIZATION, jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"motDePasseActuel\": \"faux\"}".formatted(nouveau)))
                .andExpect(status().isUnauthorized());

        assertThat(jdbc.queryForObject("select email_en_attente from utilisateur where email = ?",
                String.class, ancien)).isNull();
    }

    @Test
    void lAncienEmailResteActifJusquALaConfirmationPuisSeulLeNouveauFonctionne() throws Exception {
        mockMvc.perform(put("/auth/me").header(HttpHeaders.AUTHORIZATION, jeton())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"motDePasseActuel\": \"%s\"}".formatted(nouveau, MOT_DE_PASSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ancien))
                .andExpect(jsonPath("$.emailVerifie").value(true))
                .andExpect(jsonPath("$.emailEnAttente").value(nouveau));

        // Avant confirmation : l'ancien email fonctionne, le nouveau non.
        assertThat(connexion(ancien)).isEqualTo(200);
        assertThat(connexion(nouveau)).isEqualTo(401);

        mockMvc.perform(get("/auth/verify-email").param("token", lienRecuPar(nouveau)))
                .andExpect(status().isFound());

        // Apres confirmation : seul le nouveau email permet de se connecter.
        assertThat(connexion(nouveau)).isEqualTo(200);
        assertThat(connexion(ancien)).isEqualTo(401);
        verify(serveurSmtp, atLeastOnce()).send(any(MimeMessage.class));
    }
}
