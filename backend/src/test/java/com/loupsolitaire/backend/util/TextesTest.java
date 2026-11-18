package com.loupsolitaire.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TextesTest {

    @AfterEach
    void sortirDeLaRequete() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requeteAvecLangue(String langue) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        if (langue != null) {
            requete.addHeader(Textes.ENTETE_LANGUE, langue);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
    }

    @Test
    void anglaisSiLaRequeteLeDemande() {
        requeteAvecLangue("en");

        assertThat(Textes.selon("Poignard", "Dagger")).isEqualTo("Dagger");
    }

    @Test
    void francaisSansEnTeteOuAvecUneAutreLangue() {
        requeteAvecLangue(null);
        assertThat(Textes.selon("Poignard", "Dagger")).isEqualTo("Poignard");

        requeteAvecLangue("fr");
        assertThat(Textes.selon("Poignard", "Dagger")).isEqualTo("Poignard");

        requeteAvecLangue("de");
        assertThat(Textes.selon("Poignard", "Dagger")).isEqualTo("Poignard");
    }

    @Test
    void francaisHorsRequeteHttp() {
        assertThat(Textes.selon("Poignard", "Dagger")).isEqualTo("Poignard");
    }

    @Test
    void francaisSiLAnglaisNEstPasCharge() {
        requeteAvecLangue("en");

        assertThat(Textes.selon("Poignard", null)).isEqualTo("Poignard");
    }

    @Test
    void uneDescriptionAnglaiseVideResteVide() {
        requeteAvecLangue("en");

        assertThat(Textes.selon("Un poignard pour se defendre", "")).isEmpty();
    }
}
