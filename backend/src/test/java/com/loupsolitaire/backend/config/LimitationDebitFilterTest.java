package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.loupsolitaire.backend.service.LimiteurDeDebit;

import tools.jackson.databind.json.JsonMapper;

class LimitationDebitFilterTest {

    private LimitationDebitFilter filtre;

    @BeforeEach
    void setUp() {
        LimitationDebitProperties proprietes = ProprietesDeTest.limitationDebit();
        filtre = new LimitationDebitFilter(
                new LimiteurDeDebit(proprietes), new AdresseClient(proprietes), JsonMapper.builder().build());
    }

    private MockHttpServletResponse appeler(String methode, String uri, String ip) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest(methode, uri);
        requete.setRemoteAddr("10.0.0.1");
        requete.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, new MockFilterChain());
        return reponse;
    }

    @Test
    void leOnziemeLoginEnUneMinuteDepuisLaMemeIpRecoit429() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(appeler("POST", "/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse refus = appeler("POST", "/auth/login", "203.0.113.7");

        assertThat(refus.getStatus()).isEqualTo(429);
        assertThat(refus.getHeader("Retry-After")).isNotBlank();
        assertThat(refus.getContentAsString()).contains("Trop de requetes");
        // Une autre IP n'est pas concernee.
        assertThat(appeler("POST", "/auth/login", "198.51.100.4").getStatus()).isEqualTo(200);
    }

    @Test
    void laSixiemeInscriptionEnUneHeureDepuisLaMemeIpRecoit429() throws Exception {
        for (int i = 0; i < 5; i++) {
            appeler("POST", "/auth/register", "203.0.113.7");
        }

        assertThat(appeler("POST", "/auth/register", "203.0.113.7").getStatus()).isEqualTo(429);
    }

    @Test
    void lesRoutesDeJeuNeSontPasLimitees() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertThat(appeler("GET", "/personnages", "203.0.113.7").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void uneAdresseInventeeAGaucheDeXForwardedForNeContournePasLaLimite() throws Exception {
        // L'attaquant change la partie gauche a chaque requete ; le proxy de
        // confiance ajoute la vraie adresse a droite.
        for (int i = 0; i < 10; i++) {
            appeler("POST", "/auth/login", "1.2.3." + i + ", 203.0.113.7");
        }

        assertThat(appeler("POST", "/auth/login", "9.9.9.9, 203.0.113.7").getStatus()).isEqualTo(429);
    }

    @Test
    void adresseClientPrivilegieL_enTeteDedieQuandIlEstConfigure() {
        LimitationDebitProperties avecEntete = new LimitationDebitProperties(
                true, "True-Client-IP", 1, Map.copyOf(ProprietesDeTest.limitationDebit().regles()));
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/auth/login");
        requete.addHeader("True-Client-IP", "203.0.113.7");
        requete.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(new AdresseClient(avecEntete).de(requete)).isEqualTo("203.0.113.7");
    }

    @Test
    void sansProxyDeConfianceXForwardedForEstIgnore() {
        LimitationDebitProperties sansProxy = new LimitationDebitProperties(
                true, "", 0, ProprietesDeTest.limitationDebit().regles());
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/auth/login");
        requete.setRemoteAddr("192.0.2.10");
        requete.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(new AdresseClient(sansProxy).de(requete)).isEqualTo("192.0.2.10");
    }
}