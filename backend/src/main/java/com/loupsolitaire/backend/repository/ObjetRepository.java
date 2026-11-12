package com.loupsolitaire.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Objet;

public interface ObjetRepository extends JpaRepository<Objet, String> {

    // @EntityGraph charge "effets" dans la meme requete : evite un
    // LazyInitializationException quand ObjetService lit objet.getEffets()
    // dans une transaction differente de celle qui a charge l'Objet
    // (typiquement : l'Objet est recupere dans le controleur avant d'appeler
    // InventaireService, qui ouvre sa propre transaction).
    @EntityGraph(attributePaths = "effets")
    @Override
    Optional<Objet> findById(String id);
}