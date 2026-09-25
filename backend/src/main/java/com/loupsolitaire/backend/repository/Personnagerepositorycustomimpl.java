package com.loupsolitaire.backend.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.loupsolitaire.backend.model.Personnage;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

// Implementation manuelle plutot qu'un @Lock sur une @Query : un @Lock
// s'applique a TOUTES les entites chargees par la requete, y compris celles
// ramenees par l'EntityGraph (Chapitre, Objet, Utilisateur), qui n'ont pas de
// version -> Hibernate refuse ("has no version and may not be locked at
// level OPTIMISTIC_FORCE_INCREMENT"). Ici, seul le Personnage est verrouille.
class PersonnageRepositoryCustomImpl implements PersonnageRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Personnage> findByIdPourModification(UUID id) {
        // Meme chargement que PersonnageRepository.findById.
        EntityGraph<Personnage> graphe = entityManager.createEntityGraph(Personnage.class);
        graphe.addAttributeNodes("disciplines", "armeMaitrisee", "chapitreActuel", "utilisateur");

        Personnage personnage = entityManager.find(
                Personnage.class, id, Map.of("jakarta.persistence.fetchgraph", graphe));

        if (personnage == null) {
            return Optional.empty();
        }

        // Verrouille UNIQUEMENT le personnage : sa version sera incrementee a
        // la validation de la transaction, que le personnage ait ete modifie
        // ou non.
        entityManager.lock(personnage, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        return Optional.of(personnage);
    }
}