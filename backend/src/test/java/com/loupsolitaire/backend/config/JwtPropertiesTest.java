package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void accepteUneCleBase64DAuMoins32Octets() {
        JwtProperties proprietes = new JwtProperties(ProprietesDeTest.SECRET_JWT, 900_000L, 30);

        assertThat(proprietes.cleDeSignature().getEncoded()).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void refuseUneCleQuiNEstPasDuBase64() {
        // Ancienne valeur par defaut de application.properties.
        assertThatThrownBy(() -> new JwtProperties("CHANGE_ME_dev_only_secret_do_not_use_in_prod_1234567890", 900_000L, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refuseUneCleTropCourte() {
        // "courte" en Base64 : 6 octets seulement.
        assertThatThrownBy(() -> new JwtProperties("Y291cnRl", 900_000L, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 octets");
    }

    @Test
    void refuseLaCleDeDevPublieeDansLeDepot() {
        // SEC-03 : ancienne valeur par defaut, Base64 valide et assez longue.
        assertThatThrownBy(() -> new JwtProperties(
                "bG91cC1zb2xpdGFpcmUtREVWLU9OTFktc2VjcmV0LW5ldmVyLXVzZS1pbi1wcm9k", 900_000L, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publiee");
    }
}