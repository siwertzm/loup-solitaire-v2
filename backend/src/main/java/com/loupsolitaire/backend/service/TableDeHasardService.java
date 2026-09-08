package com.loupsolitaire.backend.service;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

// Reproduit la "Table de Hasard" du livre-jeu : un tirage 0-9. C'est le meme
// mecanisme que celui deja utilise partout dans chapitre.json (conditions de
// type "hasard"), reutilise ici pour la creation de personnage (stats, or,
// objet de depart) et pour l'arme de Maitrise des Armes.
@Service
@RequiredArgsConstructor
public class TableDeHasardService {

    private final Random random;

    // Un chiffre entre 0 et 9 inclus, comme un tirage sur la vraie Table de
    // Hasard du livre.
    public int tirerChiffre() {
        return random.nextInt(10);
    }

    // Tirage uniforme parmi une liste d'options (ex. les 9 armes du
    // catalogue pour la Discipline Kai de Maitrise des Armes).
    public <T> T tirerParmi(List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Impossible de tirer dans une liste vide ou nulle");
        }
        return options.get(random.nextInt(options.size()));
    }
}