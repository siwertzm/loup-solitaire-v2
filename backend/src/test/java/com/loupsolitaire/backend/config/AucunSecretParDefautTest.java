package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * SEC-03 : aucun secret ne doit avoir de valeur par defaut dans
 * application.properties. Sinon, une variable oubliee sur Render ferait
 * demarrer la production avec une valeur publiee dans le depot.
 */
class AucunSecretParDefautTest {

    // ${VARIABLE:defaut} -> groupe 1 = defaut (absent si pas de ":")
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[A-Z0-9_]+(?::([^}]*))?}");

    @Test
    void aucunSecretNiMotDePasseNAUneValeurParDefaut() throws IOException {
        Properties proprietes = chargerApplicationProperties();

        proprietes.stringPropertyNames().stream()
                .filter(AucunSecretParDefautTest::estUnSecret)
                .forEach(cle -> {
                    Matcher m = PLACEHOLDER.matcher(proprietes.getProperty(cle).trim());
                    assertThat(m.matches())
                            .as("%s doit venir d'une variable d'environnement", cle)
                            .isTrue();
                    String defaut = m.group(1);
                    assertThat(defaut == null || defaut.isEmpty())
                            .as("%s ne doit pas avoir de valeur par defaut (trouve : %s)", cle, defaut)
                            .isTrue();
                });
    }

    @Test
    void leSecretJwtEtLeMotDePasseDeLaBaseSontBienVerifies() throws IOException {
        Properties proprietes = chargerApplicationProperties();

        assertThat(proprietes.getProperty("jwt.secret")).isEqualTo("${JWT_SECRET:}");
        assertThat(proprietes.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
    }

    private static boolean estUnSecret(String cle) {
        // Derniere partie de la cle : "spring.mail.password", "jwt.secret"...
        // mais pas "app.password-reset.max-attempts".
        String derniere = cle.substring(cle.lastIndexOf('.') + 1).toLowerCase();
        return derniere.equals("password") || derniere.equals("secret");
    }

    private static Properties chargerApplicationProperties() throws IOException {
        Properties proprietes = new Properties();
        try (InputStream in = AucunSecretParDefautTest.class.getResourceAsStream("/application.properties")) {
            assertThat(in).as("application.properties introuvable").isNotNull();
            proprietes.load(in);
        }
        return proprietes;
    }
}