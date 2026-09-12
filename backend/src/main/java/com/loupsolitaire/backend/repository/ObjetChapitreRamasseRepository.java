package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.ObjetChapitreRamasse;

public interface ObjetChapitreRamasseRepository extends JpaRepository<ObjetChapitreRamasse, UUID> {

    Optional<ObjetChapitreRamasse> findByPersonnageIdAndChapitreIdAndObjetId(
            UUID personnageId, Integer chapitreId, String objetId);

    // Recupere tout en un appel pour un chapitre donne, pour calculer les
    // quantites restantes de tous ses objets optionnels sans une requete par objet.
    List<ObjetChapitreRamasse> findByPersonnageIdAndChapitreId(UUID personnageId, Integer chapitreId);
}