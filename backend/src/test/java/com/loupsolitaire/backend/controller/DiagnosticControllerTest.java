package com.loupsolitaire.backend.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;

class DiagnosticControllerTest {

    @Test
    void repond404TantQueLeTestNEstPasActive() {
        DiagnosticController controller = new DiagnosticController(false);

        assertThatThrownBy(controller::erreurTest).isInstanceOf(RessourceNonTrouveeException.class);
    }

    @Test
    void leveUneErreurNonGereeQuandLeTestEstActive() {
        // IllegalStateException n'a pas de @ExceptionHandler : elle donne une
        // 500 et remonte dans Sentry.
        DiagnosticController controller = new DiagnosticController(true);

        assertThatThrownBy(controller::erreurTest)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPS-04");
    }
}