package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * SEC-03 : sans JWT_SECRET, le contexte Spring refuse de demarrer (au lieu
 * de tourner avec une cle vide ou par defaut). Meme liaison des proprietes
 * "jwt.*" que l'application, sans charger tout le reste.
 */
class DemarrageSansSecretTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class ProprietesJwt {
    }

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ProprietesJwt.class)
            .withPropertyValues("jwt.expiration-ms=900000", "jwt.refresh-expiration-days=30");

    @Test
    void sansJwtSecretLeContexteRefuseDeDemarrer() {
        contexte.withPropertyValues("jwt.secret=")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).hasStackTraceContaining("JWT_SECRET est obligatoire");
                });
    }

    @Test
    void avecLaCleDeDevPublieeLeContexteRefuseDeDemarrer() {
        contexte.withPropertyValues("jwt.secret=bG91cC1zb2xpdGFpcmUtREVWLU9OTFktc2VjcmV0LW5ldmVyLXVzZS1pbi1wcm9k")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void avecUneCleValideLeContexteDemarre() {
        contexte.withPropertyValues("jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi")
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(JwtProperties.class));
    }
}