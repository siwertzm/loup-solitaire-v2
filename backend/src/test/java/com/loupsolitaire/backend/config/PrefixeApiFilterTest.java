package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

/**
 * QUAL-02 : le prefixe /api/v1 est retire avant le reste de l'application,
 * les anciens chemins passent sans changement.
 */
class PrefixeApiFilterTest {

    private final PrefixeApiFilter filtre = new PrefixeApiFilter();

    private HttpServletRequest filtrer(MockHttpServletRequest requete) throws Exception {
        AtomicReference<HttpServletRequest> recue = new AtomicReference<>();
        FilterChain chaine = (req, res) -> recue.set((HttpServletRequest) req);
        filtre.doFilter(requete, new MockHttpServletResponse(), chaine);
        return recue.get();
    }

    private static MockHttpServletRequest requete(String uri) {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", uri);
        requete.setServletPath(uri);
        return requete;
    }

    @Test
    void retireLePrefixe() throws Exception {
        HttpServletRequest recue = filtrer(requete("/api/v1/personnages/42"));

        assertThat(recue.getRequestURI()).isEqualTo("/personnages/42");
        assertThat(recue.getServletPath()).isEqualTo("/personnages/42");
        assertThat(recue.getRequestURL()).hasToString("http://localhost/personnages/42");
    }

    @Test
    void lePrefixeSeulDevientLaRacine() throws Exception {
        assertThat(filtrer(requete("/api/v1")).getRequestURI()).isEqualTo("/");
    }

    @Test
    void lesAnciensCheminsPassentSansChangement() throws Exception {
        MockHttpServletRequest ancien = requete("/personnages");
        assertThat(filtrer(ancien)).isSameAs(ancien);

        // "/api/v10" n'est pas sous le prefixe "/api/v1".
        MockHttpServletRequest voisin = requete("/api/v10/personnages");
        assertThat(filtrer(voisin)).isSameAs(voisin);
    }

    @Test
    void gardeLeContexteDeLApplication() throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/loup/api/v1/objets");
        requete.setContextPath("/loup");

        HttpServletRequest recue = filtrer(requete);

        assertThat(recue.getRequestURI()).isEqualTo("/loup/objets");
        assertThat(recue.getServletPath()).isEqualTo("/objets");
    }

    @Test
    void lUrlCompleteNIndiqueLePortQueSIlNEstPasLePortParDefaut() throws Exception {
        MockHttpServletRequest https = requete("/api/v1/version");
        https.setScheme("https");
        https.setServerName("loup.example");
        https.setServerPort(443);
        assertThat(filtrer(https).getRequestURL()).hasToString("https://loup.example/version");

        MockHttpServletRequest local = requete("/api/v1/version");
        local.setServerPort(8080);
        assertThat(filtrer(local).getRequestURL()).hasToString("http://localhost:8080/version");

        MockHttpServletRequest httpsAutrePort = requete("/api/v1/version");
        httpsAutrePort.setScheme("https");
        httpsAutrePort.setServerPort(8443);
        assertThat(filtrer(httpsAutrePort).getRequestURL()).hasToString("https://localhost:8443/version");
    }
}
