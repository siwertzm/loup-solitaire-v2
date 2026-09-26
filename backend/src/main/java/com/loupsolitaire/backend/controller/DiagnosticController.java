package com.loupsolitaire.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;

/**
 * OPS-04 : verification de la chaine d'alerte en production.
 *
 * GET /diagnostic/erreur-test leve une erreur non geree (reponse 500),
 * qui doit remonter dans Sentry et declencher l'alerte email. Inactif par
 * defaut : la route repond 404 tant que app.diagnostic.erreur-test (variable
 * DIAGNOSTIC_ERREUR_TEST) n'est pas a true. A activer le temps du test, puis
 * a remettre a false.
 */
@RestController
@RequestMapping("/diagnostic")
public class DiagnosticController {

    private final boolean erreurTestActive;

    public DiagnosticController(@Value("${app.diagnostic.erreur-test:false}") boolean erreurTestActive) {
        this.erreurTestActive = erreurTestActive;
    }

    @GetMapping("/erreur-test")
    public void erreurTest() {
        if (!erreurTestActive) {
            throw new RessourceNonTrouveeException("Ressource introuvable");
        }
        throw new IllegalStateException("Erreur de test OPS-04 : verification de l'alerte Sentry");
    }
}