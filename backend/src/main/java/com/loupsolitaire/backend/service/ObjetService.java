package com.loupsolitaire.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets attaches a un Objet du catalogue (objet.json) au
// personnage qui l'obtient.
//
// - ENDURANCE : toujours REEL (jamais temporaire), applique a
//   enduranceActuelle. Si l'objet est OBJETS_SPECIAUX (armure : casque,
//   cotte de mailles), l'effet est en plus PASSIF et lie a la possession :
//   il touche aussi enduranceMax, et se retire si l'objet est perdu.
//
// - HABILETE : toujours TEMPORAIRE (ex. essence d'Alether), ajoute a
//   Personnage.habiliteTemp plutot qu'a habilite. Remis a zero a chaque
//   changement de chapitre (voir PersonnageService.reinitialiserHabiliteTemp,
//   a appeler par le futur service de navigation entre chapitres). Jamais
//   annule dans retirerBonusPerte : inutile, ca se reinitialise tout seul.
//
// A appeler explicitement en plus de InventaireService.ajouterObjet(...) :
// volontairement PAS integre dans InventaireService pour eviter d'y
// accumuler des responsabilites deconnectees de la gestion de capacite.
@Service
@RequiredArgsConstructor
public class ObjetService {

    private final PersonnageRepository personnageRepository;

    @Transactional
    public void appliquerBonusRecuperation(Personnage personnage, Objet objet) {
        for (Effet effet : objet.getEffets()) {
            if (effet.getType() == TypeEffet.ENDURANCE) {
                appliquerEndurance(personnage, objet, effet.getValeur());
            } else if (effet.getType() == TypeEffet.HABILETE) {
                personnage.setHabiliteTemp(personnage.getHabiliteTemp() + effet.getValeur());
            }
            // REPAS/VOL n'ont pas de sens comme effet d'un Objet du
            // catalogue (uniquement rencontres comme effets de Chapitre).
        }
        personnageRepository.save(personnage);
    }

    // Retire le bonus passif d'une armure perdue/volee. Ne gere que
    // l'ENDURANCE : l'HABILETE etant toujours temporaire (habiliteTemp),
    // il n'y a jamais rien a "annuler" ici, elle se reinitialise seule au
    // prochain changement de chapitre.
    @Transactional
    public void retirerBonusPerte(Personnage personnage, Objet objet) {
        if (objet.getCategorie() != CategorieObjet.OBJETS_SPECIAUX) {
            return;
        }

        boolean aChange = false;
        for (Effet effet : objet.getEffets()) {
            if (effet.getType() == TypeEffet.ENDURANCE) {
                appliquerEndurance(personnage, objet, -effet.getValeur());
                aChange = true;
            }
        }
        if (aChange) {
            personnageRepository.save(personnage);
        }
    }

    private void appliquerEndurance(Personnage personnage, Objet objet, int delta) {
        if (objet.getCategorie() == CategorieObjet.OBJETS_SPECIAUX) {
            // Armure : le plafond bouge avec le delta.
            personnage.setEnduranceMax(personnage.getEnduranceMax() + delta);
        }
        int nouvelleActuelle = personnage.getEnduranceActuelle() + delta;
        nouvelleActuelle = Math.min(nouvelleActuelle, personnage.getEnduranceMax());
        nouvelleActuelle = Math.max(nouvelleActuelle, 0);
        personnage.setEnduranceActuelle(nouvelleActuelle);
    }
}