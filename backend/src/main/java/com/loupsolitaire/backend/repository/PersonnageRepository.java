package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;

public interface PersonnageRepository extends JpaRepository<Personnage, UUID> {

    // @EntityGraph charge disciplines/armeMaitrisee/chapitreActuel/utilisateur
    // dans la meme requete : evite un LazyInitializationException quand le
    // controleur lit ces champs une fois la session Hibernate fermee.
    // "utilisateur" est necessaire ici (pas sur findByUtilisateur ci-dessous)
    // car recuperer() verifie la propriete via personnage.getUtilisateur().
    @EntityGraph(attributePaths = {"disciplines", "armeMaitrisee", "chapitreActuel", "utilisateur"})
    @Override
    Optional<Personnage> findById(UUID id);

    @EntityGraph(attributePaths = {"disciplines", "armeMaitrisee", "chapitreActuel"})
    List<Personnage> findByUtilisateur(Utilisateur utilisateur);
}