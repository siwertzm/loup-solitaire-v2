package com.loupsolitaire.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.response.VersionResponse;

import lombok.RequiredArgsConstructor;

/**
 * QUAL-02 : route publique lue par l'app au demarrage. Les apps installees ne
 * se mettent pas a jour seules : une app plus ancienne que la version minimale
 * affiche un ecran "mise a jour requise" au lieu d'appeler une API dont le
 * contrat a change.
 */
@RestController
@RequestMapping("/version")
@RequiredArgsConstructor
public class VersionController {

    private final AppProperties appProperties;

    @GetMapping
    public VersionResponse version() {
        return new VersionResponse(appProperties.versionMinimale());
    }
}
