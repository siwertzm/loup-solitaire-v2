package com.loupsolitaire.backend.service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.loupsolitaire.backend.model.Combat;

/**
 * Donnees necessaires pour evaluer les conditions d'un chapitre, chargees AU
 * PLUS UNE FOIS par requete (voir ConditionService.nouveauContexte).
 *
 * Sans ce contexte, GET /chapitre relisait tout l'inventaire en base pour
 * CHAQUE condition OBJET/ARME/BOURSE, et le combat pour CHAQUE condition de
 * combat : un chapitre a 4 liens conditionnes par des objets declenchait 4
 * fois la meme requete.
 *
 * Chargement paresseux : un chapitre sans condition d'objet ne lit jamais
 * l'inventaire, un chapitre sans condition de combat ne lit jamais le combat.
 *
 * Instantane en lecture : a n'utiliser que le temps d'une evaluation, sans
 * modifier l'inventaire ou le combat entre-temps. Pas thread-safe (une
 * requete = un thread).
 */
public final class ContexteConditions {

    private final Supplier<Map<String, Integer>> chargeurQuantites;
    private final Supplier<Optional<Combat>> chargeurCombat;

    private Map<String, Integer> quantites;
    // Pas de champ Optional (Sonar java:S2789) : un booleen dit si le combat
    // a deja ete lu, et combat vaut null s'il n'y en a pas.
    private boolean combatCharge;
    private Combat combat;

    ContexteConditions(Supplier<Map<String, Integer>> chargeurQuantites,
                       Supplier<Optional<Combat>> chargeurCombat) {
        this.chargeurQuantites = chargeurQuantites;
        this.chargeurCombat = chargeurCombat;
    }

    int quantitePossedee(String objetId) {
        if (quantites == null) {
            quantites = chargeurQuantites.get();
        }
        return quantites.getOrDefault(objetId, 0);
    }

    Optional<Combat> combatDuChapitreActuel() {
        if (!combatCharge) {
            combat = chargeurCombat.get().orElse(null);
            combatCharge = true;
        }
        return Optional.ofNullable(combat);
    }
}