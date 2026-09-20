package com.loupsolitaire.backend.response;

// Une arrivee du journal : le chapitre traverse, le debut de son texte
// (deja nettoye du balisage, voir JournalService.extraire, pense pour tenir
// sur une ligne) et trois indicateurs pour afficher des pictogrammes :
// le chapitre comporte-t-il un combat, des effets, des objets ? Ce sont les
// donnees du chapitre lui-meme, pas de l'etat du joueur (un effet
// conditionnel compte, meme s'il ne s'applique pas a ce joueur).
// extrait : chaine vide si le texte du chapitre est introuvable.
// mort : le personnage est mort A CETTE ARRIVEE (chapitre de mort narrative,
// perte d'endurance ou defaite en combat). C'est propre a l'arrivee et non au
// chapitre : un chapitre traverse deux fois peut n'etre mortel qu'une fois.
public record ChapitreParcouruResponse(
        Integer chapitreId,
        String extrait,
        boolean avecCombat,
        boolean avecEffets,
        boolean avecObjets,
        boolean mort) {
}