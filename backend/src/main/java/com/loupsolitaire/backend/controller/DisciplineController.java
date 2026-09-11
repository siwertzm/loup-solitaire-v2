package com.loupsolitaire.backend.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.response.DisciplineResponse;

import lombok.RequiredArgsConstructor;

// Referentiel en lecture seule : les 10 disciplines Kai (nom + description
// completes), chargees en base au demarrage depuis discipline.json. Utilise
// par l'ecran de creation de personnage, pour ne pas dupliquer ce contenu
// cote client.
@RestController
@RequestMapping("/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineRepository disciplineRepository;

    @GetMapping
    public List<DisciplineResponse> lister() {
        return disciplineRepository.findAll().stream()
                .sorted(Comparator.comparingInt(d -> d.getId().ordinal()))
                .map(DisciplineResponse::fromEntity)
                .toList();
    }
}