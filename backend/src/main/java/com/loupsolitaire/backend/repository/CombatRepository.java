package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.StatutCombat;

public interface CombatRepository extends JpaRepository<Combat, UUID> {

    // @EntityGraph charge ennemis + leur reference catalogue (ennemis.ennemi)
    // dans la meme requete : un Combat traverse plusieurs @Transactional
    // (CombatService.initierCombat -> jouerTour -> CombatMapper), donc ses
    // collections LAZY doivent etre deja chargees avant de quitter la
    // premiere transaction, sous peine de LazyInitializationException.
    //
    // Au plus un combat EN_COURS par (personnage, chapitre) : impose cote
    // service (CombatService.initierCombat), pas par contrainte SQL.
    @EntityGraph(attributePaths = {"ennemis", "ennemis.ennemi"})
    Optional<Combat> findByPersonnageIdAndChapitreIdAndStatut(
            UUID personnageId, Integer chapitreId, StatutCombat statut);

    // Le plus recent, resolu ou non : utilise par ConditionService pour
    // evaluer FUITE/ASSAUT_MAX/ASSAUT_ECHEC/ENDURANCE_PERDUE une fois le
    // combat termine (le personnage est encore sur ce chapitre le temps de
    // choisir son lien de sortie).
    @EntityGraph(attributePaths = {"ennemis", "ennemis.ennemi"})
    Optional<Combat> findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(Personnage personnage, Integer chapitreId);
}