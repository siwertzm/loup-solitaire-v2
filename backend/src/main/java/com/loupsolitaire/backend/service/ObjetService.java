package com.loupsolitaire.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets attaches a un Objet du catalogue (objet.json).
//
// - OBJETS_SPECIAUX (armure : casque, cotte de mailles) : effet PASSIF,
//   lie a la possession. Applique a la RECUPERATION, retire a la PERTE.
//   Touche enduranceMax (le plafond) en plus d'enduranceActuelle.
//
// - OBJET (consommable : potion de guerison, laumspur, essence d'Alether) :
//   AUCUN effet a la recuperation, ni au retrait/perte generique. L'effet
//   ne s'applique qu'a la CONSOMMATION explicite (voir
//   appliquerEffetsConsommation), qui ne retire PAS l'objet elle-meme :
//   c'est au controleur d'enchainer avec InventaireService.retirerObjet,
//   pour eviter une dependance circulaire entre les deux services.
//
// HABILITE : toujours TEMPORAIRE (habiliteTemp), remis a zero a chaque
// changement de chapitre (voir PersonnageService.reinitialiserHabiliteTemp).
@Service
@RequiredArgsConstructor
public class ObjetService {

    private final PersonnageRepository personnageRepository;

    // Uniquement pour l'armure (OBJETS_SPECIAUX) : les consommables
    // (OBJET) n'ont plus aucun effet a la recuperation.
    @Transactional
    public void appliquerBonusRecuperation(Personnage personnage, Objet objet) {
        if (objet.getCategorie() != CategorieObjet.OBJETS_SPECIAUX) {
            return;
        }
        appliquerEffets(personnage, objet, 1);
        personnageRepository.save(personnage);
    }

    // Symetrique : uniquement pour l'armure. Rien a faire pour un
    // consommable, qui n'a jamais eu d'effet applique a la recuperation.
    @Transactional
    public void retirerBonusPerte(Personnage personnage, Objet objet) {
        if (objet.getCategorie() != CategorieObjet.OBJETS_SPECIAUX) {
            return;
        }
        appliquerEffets(personnage, objet, -1);
        personnageRepository.save(personnage);
    }

    // Applique les effets d'un consommable au moment ou le joueur choisit
    // explicitement de le consommer (boire la potion, manger le repas...).
    // Reserve a la categorie OBJET : armes, bourse et objets speciaux
    // (armure) ne se "consomment" pas.
    // Ne retire PAS l'objet de l'inventaire : a faire ensuite via
    // InventaireService.retirerObjet (orchestre par le controleur).
    @Transactional
    public void appliquerEffetsConsommation(Personnage personnage, Objet objet) {
        if (personnage.isMort()) {
            throw new IllegalArgumentException(
                    "Ce personnage est mort (perte d'endurance) : ressuscitez-le via "
                            + "POST /personnages/{id}/ressusciter avant de continuer");
        }
        if (objet.getCategorie() != CategorieObjet.OBJET) {
            throw new IllegalArgumentException(
                    "Impossible de consommer un objet de categorie " + objet.getCategorie());
        }
        appliquerEffets(personnage, objet, 1);
        personnageRepository.save(personnage);
    }

    private void appliquerEffets(Personnage personnage, Objet objet, int signe) {
        for (Effet effet : objet.getEffets()) {
            switch (effet.getType()) {
                case ENDURANCE -> appliquerEndurance(personnage, objet, signe * effet.getValeur());
                case HABILITE -> personnage.setHabiliteTemp(personnage.getHabiliteTemp() + signe * effet.getValeur());
                default -> {
                    // Les autres types d'effet ne modifient pas les caracteristiques.
                }
            }
        }
    }

    private void appliquerEndurance(Personnage personnage, Objet objet, int delta) {
        if (objet.getCategorie() == CategorieObjet.OBJETS_SPECIAUX) {
            personnage.setEnduranceMax(personnage.getEnduranceMax() + delta);
        }
        int nouvelleActuelle = personnage.getEnduranceActuelle() + delta;
        nouvelleActuelle = Math.min(nouvelleActuelle, personnage.getEnduranceMax());
        nouvelleActuelle = Math.max(nouvelleActuelle, 0);
        personnage.setEnduranceActuelle(nouvelleActuelle);
    }
}