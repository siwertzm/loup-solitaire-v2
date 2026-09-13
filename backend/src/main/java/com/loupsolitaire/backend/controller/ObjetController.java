package com.loupsolitaire.backend.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.response.ObjetResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/objets")
@RequiredArgsConstructor
public class ObjetController {

    private final ObjetRepository objetRepository;

    // @Transactional necessaire : Objet.effets est LAZY (defaut JPA pour
    // @OneToMany), et ObjetResponse.fromEntity y accede. Sans transaction
    // ouverte pendant le mapping, LazyInitializationException (la session
    // Hibernate de findAll() est deja fermee une fois le stream execute).
    @GetMapping
    @Transactional(readOnly = true)
    public List<ObjetResponse> lister() {
        return objetRepository.findAll().stream()
                .sorted(Comparator.comparing(o -> o.getNom().toLowerCase()))
                .map(ObjetResponse::fromEntity)
                .toList();
    }
}