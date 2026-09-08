package com.loupsolitaire.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;

public interface ObjetRepository extends JpaRepository<Objet, String> {

    // Pour le tirage de l'arme de Maitrise des Armes, parmi toutes les armes.
    List<Objet> findByCategorie(CategorieObjet categorie);
}