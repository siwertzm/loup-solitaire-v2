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
}