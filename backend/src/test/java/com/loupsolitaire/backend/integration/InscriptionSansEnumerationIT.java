package com.loupsolitaire.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * SEC-05, sur PostgreSQL : l'inscription repond exactement la meme chose
 * pour un email deja inscrit et pour un email nouveau ; seul un nom
 * d'utilisateur pris donne un 409.
 */
class InscriptionSansEnumerationIT extends IntegrationPostgres {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final String suffixe = UUID.randomUUID().toString().substring(0, 8);
    private final String emailExistant = "existant-" + suffixe + "@test.local";

    @BeforeEach
    void unCompteExiste() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        inscrire("premier-" + suffixe, emailExistant);
    }

    @AfterEach
    void nettoyer() {
        String condition = "(select id from utilisateur where username like ?)";
        jdbc.update("delete from email_verification_token where utilisateur_id in " + condition, "%-" + suffixe);
        jdbc.update("delete from utilisateur where username like ?", "%-" + suffixe);
    }

    private MockHttpServletResponse inscrire(String username, String email) throws Exception {
        return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "email": "%s", "password": "motdepasse-sec05"}
                        """.formatted(username, email)))
                .andReturn().getResponse();
    }

    @Test
    void memeReponsePourUnEmailDejaInscritEtUnEmailNouveau() throws Exception {
        MockHttpServletResponse emailConnu = inscrire("deuxieme-" + suffixe, emailExistant);
        MockHttpServletResponse emailNouveau = inscrire("troisieme-" + suffixe, "nouveau-" + suffixe + "@test.local");

        assertThat(emailConnu.getStatus()).isEqualTo(202).isEqualTo(emailNouveau.getStatus());
        assertThat(emailConnu.getContentAsString()).isEmpty();
        assertThat(emailNouveau.getContentAsString()).isEmpty();
        // L'email deja inscrit n'a pas cree de second compte.
        assertThat(jdbc.queryForObject("select count(*) from utilisateur where email = ?", Long.class, emailExistant))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from utilisateur where username = ?", Long.class,
                "deuxieme-" + suffixe)).isZero();
    }

    @Test
    void unNomDUtilisateurPrisDonne409() throws Exception {
        assertThat(inscrire("premier-" + suffixe, "autre-" + suffixe + "@test.local").getStatus()).isEqualTo(409);
    }
}
