package com.loupsolitaire.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.request.RegisterRequest;

class EmailsTest {

    @Test
    void metEnMinusculesEtRetireLesEspaces() {
        assertThat(Emails.normaliser("  Bob@Mail.FR ")).isEqualTo("bob@mail.fr");
    }

    @Test
    void laisseNullTelQuel() {
        assertThat(Emails.normaliser(null)).isNull();
    }

    @Test
    void neDependPasDeLaLangueDuSysteme() {
        // En turc, "I".toLowerCase() donne un "i" sans point : Locale.ROOT l'evite.
        assertThat(Emails.normaliser("IVAN@MAIL.FR")).isEqualTo("ivan@mail.fr");
    }

    @Test
    void lesRequetesNormalisentLEmailRecu() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(" Marius@Example.COM ");

        assertThat(request.getEmail()).isEqualTo("marius@example.com");
    }

    @Test
    void lUtilisateurEnregistreToujoursUnEmailNormalise() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("Marius@Example.COM");

        assertThat(utilisateur.getEmail()).isEqualTo("marius@example.com");
    }

    @Test
    void masquerNeGardeQueLaPremiereLettreEtLeDomaine() {
        // SEC-06 : adresse demandee, montree dans l'alerte a l'ancienne adresse.
        assertThat(Emails.masquer("nouveau@example.com")).isEqualTo("n***@example.com");
        assertThat(Emails.masquer("pas-une-adresse")).isEqualTo("***");
        assertThat(Emails.masquer(null)).isNull();
    }

    @Test
    void masquerDansTexteMasqueToutesLesAdresses() {
        // RGPD-03 : messages d'erreur journalises (serveur d'email, etc.).
        assertThat(Emails.masquerDansTexte("550 <marius@example.com> refuse, copie a bob.l@mail.fr"))
                .isEqualTo("550 <m***@example.com> refuse, copie a b***@mail.fr");
        assertThat(Emails.masquerDansTexte("Connexion refusee")).isEqualTo("Connexion refusee");
        assertThat(Emails.masquerDansTexte("")).isEmpty();
        assertThat(Emails.masquerDansTexte(null)).isNull();
    }
}
