package com.loupsolitaire.backend.controller;

import java.util.Comparator;
import java.util.List;

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

    @GetMapping
    public List<ObjetResponse> lister() {
        return objetRepository.findAll().stream()
                .sorted(Comparator.comparing(o -> o.getNom().toLowerCase()))
                .map(ObjetResponse::fromEntity)
                .toList();
    }
}