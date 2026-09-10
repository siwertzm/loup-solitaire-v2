package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Personnage;

public interface CombatRepository extends JpaRepository<Combat, UUID> {

    // Un seul combat par (personnage, chapitre), jamais recree une fois
    // qu'il existe (voir CombatService.combatEnCoursOuNouveau) : EN_COURS
    // ou deja resolu (VICTOIRE/DEFAITE/FUITE/INTERROMPU), c'est toujours
    // CE combat qui fait autorite pour ce chapitre.
    //
    // @EntityGraph charge ennemis + leur reference catalogue (ennemis.ennemi)
    // dans la meme requete : un Combat traverse plusieurs @Transactional
    // (CombatService.initierCombat -> jouerTour -> CombatMapper), donc ses
    // collections LAZY doivent etre deja chargees avant de quitter la
    // premiere transaction, sous peine de LazyInitializationException.
    @EntityGraph(attributePaths = {"ennemis", "ennemis.ennemi"})
    Optional<Combat> findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(Personnage personnage, Integer chapitreId);
}