package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.config.LimitationDebitProperties;
import com.loupsolitaire.backend.config.ProprietesDeTest;
import com.loupsolitaire.backend.exception.TropDeRequetesException;

class LimiteurDeDebitTest {

    // Horloge reglable a la main : la fenetre avance sans attendre.
    private static final class HorlogeReglable extends Clock {
        private Instant maintenant = Instant.parse("2026-09-25T20:00:00Z");

        void avancer(Duration duree) {
            maintenant = maintenant.plus(duree);
        }

        @Override
        public Instant instant() {
            return maintenant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private HorlogeReglable horloge;
    private LimiteurDeDebit limiteur;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable();
        // reset-demande-email : 3 par heure.
        limiteur = new LimiteurDeDebit(ProprietesDeTest.limitationDebit(), horloge);
    }

    @Test
    void laissePasserJusquALaLimitePuisRefuseAvecLeDelaiRestant() {
        for (int i = 0; i < 3; i++) {
            limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com");
        }
        horloge.avancer(Duration.ofMinutes(20));

        assertThatThrownBy(() -> limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com"))
                .isInstanceOfSatisfying(TropDeRequetesException.class,
                        e -> assertThat(e.getSecondesAvantNouvelEssai()).isEqualTo(40 * 60));
    }

    @Test
    void uneNouvelleFenetreRepartDeZero() {
        for (int i = 0; i < 3; i++) {
            limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com");
        }
        horloge.avancer(Duration.ofHours(1));

        assertThatCode(() -> limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void lesClesEtLesReglesSontIndependantes() {
        for (int i = 0; i < 3; i++) {
            limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com");
        }

        assertThatCode(() -> {
            limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "autre@example.com");
            limiteur.consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, "marius@example.com");
        }).doesNotThrowAnyException();
    }

    @Test
    void laCleIgnoreLaCasseEtLesEspaces() {
        for (int i = 0; i < 3; i++) {
            limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com");
        }

        assertThatThrownBy(() -> limiteur.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "  MARIUS@example.com "))
                .isInstanceOf(TropDeRequetesException.class);
    }

    @Test
    void verifierNeCompteRienEtReinitialiserRemetAZero() {
        for (int i = 0; i < 20; i++) {
            limiteur.verifier(LimiteurDeDebit.LOGIN_COMPTE, "marius");
        }
        for (int i = 0; i < 10; i++) {
            limiteur.enregistrerEchec(LimiteurDeDebit.LOGIN_COMPTE, "marius");
        }
        assertThatThrownBy(() -> limiteur.verifier(LimiteurDeDebit.LOGIN_COMPTE, "marius"))
                .isInstanceOf(TropDeRequetesException.class);

        limiteur.reinitialiser(LimiteurDeDebit.LOGIN_COMPTE, "marius");

        assertThatCode(() -> limiteur.verifier(LimiteurDeDebit.LOGIN_COMPTE, "marius"))
                .doesNotThrowAnyException();
    }

    @Test
    void desactiveNeRefuseJamais() {
        LimitationDebitProperties actif = ProprietesDeTest.limitationDebit();
        LimiteurDeDebit inactif = new LimiteurDeDebit(new LimitationDebitProperties(
                false, "", 1, actif.regles()), horloge);

        assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                inactif.consommer(LimiteurDeDebit.RESET_DEMANDE_EMAIL, "marius@example.com");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void refuseDeDemarrerSiUneRegleManque() {
        Map<String, LimitationDebitProperties.Regle> incompletes =
                new HashMap<>(ProprietesDeTest.limitationDebit().regles());
        incompletes.remove(LimiteurDeDebit.LOGIN_IP);

        assertThatThrownBy(() -> new LimiteurDeDebit(new LimitationDebitProperties(true, "", 1, incompletes)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("login-ip");
    }
}