package com.loupsolitaire.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * RGPD-03 : aucune adresse email complete dans les logs. Les logs (et les
 * rapports d'erreurs Sentry, qui reprennent les log.error) ne doivent recevoir
 * une adresse qu'au travers de Emails.masquer(...) ou Emails.masquerDansTexte(...).
 *
 * Controle du code source : un appel log.xxx(...) qui passe une variable ou un
 * getter d'email sans masquage fait echouer ce test.
 */
class LogsSansEmailTest {

    private static final Path SOURCES = Path.of("src/main/java");

    // Un appel de log, sur une ou plusieurs lignes, jusqu'au ");" final.
    private static final Pattern APPEL_DE_LOG = Pattern.compile("log\\.(trace|debug|info|warn|error)\\((.*?)\\);",
            Pattern.DOTALL);
    private static final Pattern TEXTE_ENTRE_GUILLEMETS = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*+\"");
    private static final Pattern ARGUMENT_MASQUE = Pattern.compile("Emails\\.masquer(DansTexte)?\\([^()]*+(\\([^()]*+\\))?+[^()]*+\\)");
    // Noms qui portent une adresse dans ce projet.
    private static final Pattern ADRESSE_EN_CLAIR = Pattern.compile(
            "\\b(destinataire|email|ancienneAdresse|nouvelEmail|adresse|identifiant)\\b|getEmail\\w*\\(\\)");

    @Test
    void aucunLogNeRecoitUneAdresseEmailNonMasquee() throws IOException {
        List<String> fautes = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(SOURCES)) {
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher appel = APPEL_DE_LOG.matcher(Files.readString(fichier));
                while (appel.find()) {
                    String arguments = TEXTE_ENTRE_GUILLEMETS.matcher(appel.group(2)).replaceAll("\"\"");
                    arguments = ARGUMENT_MASQUE.matcher(arguments).replaceAll("MASQUE");
                    if (ADRESSE_EN_CLAIR.matcher(arguments).find()) {
                        fautes.add(SOURCES.relativize(fichier) + " : " + appel.group().replaceAll("\\s+", " "));
                    }
                }
            }
        }
        assertThat(fautes).as("logs avec une adresse email non masquee").isEmpty();
    }

    @Test
    void leControleDetecteBienUneAdresseNonMasquee() {
        // Garde-fou : le test ci-dessus ne doit pas passer par erreur.
        String fautif = "log.error(\"Echec a {} : {}\", destinataire, e.getMessage());";
        String correct = "log.error(\"Echec a {} : {}\", Emails.masquer(destinataire), "
                + "Emails.masquerDansTexte(e.getMessage()));";

        assertThat(contientUneAdresse(fautif)).isTrue();
        assertThat(contientUneAdresse(correct)).isFalse();
        assertThat(contientUneAdresse("log.warn(\"x\", utilisateur.getEmail());")).isTrue();
    }

    private static boolean contientUneAdresse(String code) {
        Matcher appel = APPEL_DE_LOG.matcher(code);
        assertThat(appel.find()).isTrue();
        String arguments = TEXTE_ENTRE_GUILLEMETS.matcher(appel.group(2)).replaceAll("\"\"");
        arguments = ARGUMENT_MASQUE.matcher(arguments).replaceAll("MASQUE");
        return ADRESSE_EN_CLAIR.matcher(arguments).find();
    }
}
