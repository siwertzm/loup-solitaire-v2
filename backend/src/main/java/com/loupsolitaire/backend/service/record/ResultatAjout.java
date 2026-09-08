package com.loupsolitaire.backend.service.record;

import java.util.List;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;

// quantiteAjoutee peut etre inferieure a quantiteDemandee si la limite de
// categorie plafonne l'ajout (armes, objets+repas, bourse). Jamais
// d'exception dans ce cas. Quand plafonne, objetsRemplacables liste ce que
// le personnage possede deja dans cette categorie, pour que le frontend
// propose "retirer pour faire de la place ?".
public record ResultatAjout(
        Objet objet,
        int quantiteDemandee,
        int quantiteAjoutee,
        List<InventaireItem> objetsRemplacables) {

    public boolean estPlafonne() {
        return quantiteAjoutee < quantiteDemandee;
    }
}