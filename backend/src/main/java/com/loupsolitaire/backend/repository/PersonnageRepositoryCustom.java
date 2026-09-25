package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.loupsolitaire.backend.model.Personnage;

public interface PersonnageRepositoryCustom {

    /**
     * A utiliser pour toute action qui MODIFIE le personnage (ou son
     * inventaire, son combat...). Meme chargement que findById, mais la
     * version du personnage est incrementee a la fin de la transaction, meme
     * si seuls l'inventaire ou le combat ont change : deux actions simultanees
     * sur le meme personnage entrent donc toujours en conflit, et la seconde
     * est rejetee (OptimisticLockingFailureException, 409).
     *
     * Pas de "SELECT ... FOR UPDATE" : aucune requete n'attend l'autre.
     */
    Optional<Personnage> findByIdPourModification(UUID id);
}