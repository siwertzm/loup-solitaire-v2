package com.loupsolitaire.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.repository.InventaireItemRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ResultatAjout;

import lombok.RequiredArgsConstructor;

// Centralise les regles de capacite de la Feuille d'Aventure. Ces limites
// portent sur des sommes de quantites a travers plusieurs lignes : ni
// l'entite InventaireItem, ni une contrainte SQL ne peuvent verifier ca
// proprement, d'ou un service dedie.
//
// Gere aussi le recalcul de l'HABILETE effective a chaque ajout/retrait
// d'arme : -4 sans arme, +2 avec l'arme maitrisee, +0 sinon. Contrairement
// au bonus d'armure (delta par objet, gere par ObjetService), cette regle
// depend de l'etat GLOBAL des armes possedees, d'ou un recalcul complet a
// chaque fois plutot qu'un delta.
@Service
@RequiredArgsConstructor
public class InventaireService {

    private static final int MAX_ARMES = 2;
    private static final int MAX_OBJETS_ET_REPAS = 8;
    private static final int MAX_BOURSE = 50;
    private static final int MALUS_SANS_ARME = -4;
    private static final int BONUS_ARME_MAITRISEE = 2;

    // Categories regroupees pour le calcul des limites : ARME et BOURSE
    // comptent seules, OBJET+REPAS sont cumules ensemble. OBJETS_SPECIAUX
    // n'a pas de limite (coherent avec le livre).
    private static final Set<CategorieObjet> GROUPE_OBJETS_ET_REPAS = Set.of(
            CategorieObjet.OBJET, CategorieObjet.REPAS
    );

    private final InventaireItemRepository inventaireItemRepository;
    private final PersonnageRepository personnageRepository;
    private final ObjetService objetService;

    // Pour la fiche personnage : consultation en lecture seule.
    public List<InventaireItem> listerInventaire(Personnage personnage) {
        return inventaireItemRepository.findByPersonnage(personnage);
    }

    @Transactional
    public ResultatAjout ajouterObjet(Personnage personnage, Objet objet, int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantite a ajouter doit etre strictement positive");
        }

        int quantiteAjoutee = quantite;
        List<InventaireItem> objetsRemplacables = List.of();

        Integer limite = limitePour(objet.getCategorie());
        if (limite != null) {
            List<InventaireItem> itemsDuGroupe = itemsDuGroupe(personnage, objet.getCategorie());
            int quantiteActuelle = itemsDuGroupe.stream().mapToInt(InventaireItem::getQuantite).sum();
            int placeRestante = Math.max(0, limite - quantiteActuelle);
            quantiteAjoutee = Math.min(quantite, placeRestante);

            if (quantiteAjoutee < quantite) {
                // Plafonne : le frontend peut proposer de retirer un de ces
                // objets pour faire de la place.
                objetsRemplacables = itemsDuGroupe;
            }
        }

        if (quantiteAjoutee > 0) {
            InventaireItem item = inventaireItemRepository
                    .findByPersonnageAndObjetId(personnage, objet.getId())
                    .orElseGet(() -> {
                        InventaireItem nouveau = new InventaireItem();
                        nouveau.setPersonnage(personnage);
                        nouveau.setObjet(objet);
                        nouveau.setQuantite(0);
                        return nouveau;
                    });

            item.setQuantite(item.getQuantite() + quantiteAjoutee);
            inventaireItemRepository.save(item);

            objetService.appliquerBonusRecuperation(personnage, objet);
            recalculerHabiliteSiArme(personnage, objet.getCategorie());
        }

        return new ResultatAjout(objet, quantite, quantiteAjoutee, objetsRemplacables);
    }

    // Retire une quantite d'un objet deja possede pour faire de la place,
    // puis ajoute le nouvel objet. Utilise quand le joueur choisit de
    // remplacer un objet (proposition faite via ResultatAjout.objetsRemplacables).
    @Transactional
    public ResultatAjout remplacerObjet(Personnage personnage,
                                         Objet objetARetirer, int quantiteARetirer,
                                         Objet objetAAjouter, int quantiteAAjouter) {
        retirerObjet(personnage, objetARetirer, quantiteARetirer);
        return ajouterObjet(personnage, objetAAjouter, quantiteAAjouter);
    }

    // Pour les ObjetChap a valeur negative (paiement, objet detruit/vole).
    // Le Lien menant a ces chapitres garantit deja la possession suffisante
    // (condition bourse/objet), mais on revalide ici quand meme : ce service
    // ne doit pas dependre silencieusement d'une verification faite ailleurs.
    @Transactional
    public void retirerObjet(Personnage personnage, Objet objet, int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantite a retirer doit etre strictement positive");
        }

        InventaireItem item = inventaireItemRepository
                .findByPersonnageAndObjetId(personnage, objet.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Le personnage ne possede pas " + objet.getId() + ", impossible d'en retirer"));

        if (item.getQuantite() < quantite) {
            throw new IllegalStateException(
                    "Quantite insuffisante de " + objet.getId() + " (possede " + item.getQuantite()
                            + ", retrait demande " + quantite + ")");
        }

        int reste = item.getQuantite() - quantite;
        if (reste == 0) {
            inventaireItemRepository.delete(item);
            // Le bonus (armure passive) ne se retire que si le personnage
            // n'en possede plus AUCUN exemplaire. Une reduction partielle
            // (reste > 0) ne doit rien changer au bonus.
            objetService.retirerBonusPerte(personnage, objet);
        } else {
            item.setQuantite(reste);
            inventaireItemRepository.save(item);
        }

        // Contrairement au bonus d'armure (delta, seulement a la perte
        // totale), l'habilite depend de l'etat global des armes : on
        // recalcule a chaque retrait, partiel ou total.
        recalculerHabiliteSiArme(personnage, objet.getCategorie());
    }

    // Recalcule et persiste l'HABILETE effective apres un changement
    // impliquant une ARME. Ne fait rien pour les autres categories.
    private void recalculerHabiliteSiArme(Personnage personnage, CategorieObjet categorie) {
        if (categorie != CategorieObjet.ARME) {
            return;
        }
        recalculerHabiliteArmes(personnage);
    }

    // Public : recalcule l'HABILETE effective a partir des armes possedees
    // et de habiliteBase. A appeler aussi quand habiliteBase change de
    // facon PERMANENTE (voir EffetChapitreService), pas seulement lors
    // d'un ajout/retrait d'arme.
    public void recalculerHabiliteArmes(Personnage personnage) {
        List<InventaireItem> armes = inventaireItemRepository.findByPersonnage(personnage).stream()
                .filter(item -> item.getObjet().getCategorie() == CategorieObjet.ARME)
                .toList();

        int nouvelleHabilite;
        if (armes.isEmpty()) {
            nouvelleHabilite = personnage.getHabiliteBase() + MALUS_SANS_ARME;
        } else {
            boolean possedeArmeMaitrisee = personnage.getArmeMaitrisee() != null
                    && armes.stream().anyMatch(item -> item.getObjet().getId().equals(personnage.getArmeMaitrisee().getId()));
            nouvelleHabilite = possedeArmeMaitrisee
                    ? personnage.getHabiliteBase() + BONUS_ARME_MAITRISEE
                    : personnage.getHabiliteBase();
        }

        personnage.setHabilite(nouvelleHabilite);
        personnageRepository.save(personnage);
    }

    private Integer limitePour(CategorieObjet categorie) {
        return switch (categorie) {
            case ARME -> MAX_ARMES;
            case OBJET, REPAS -> MAX_OBJETS_ET_REPAS;
            case BOURSE -> MAX_BOURSE;
            case OBJETS_SPECIAUX -> null;
        };
    }

    // Objets deja possedes dans le meme groupe de limite que la categorie
    // donnee (ex. pour REPAS, inclut aussi les OBJET).
    private List<InventaireItem> itemsDuGroupe(Personnage personnage, CategorieObjet categorie) {
        return inventaireItemRepository.findByPersonnage(personnage).stream()
                .filter(item -> memeGroupe(item.getObjet().getCategorie(), categorie))
                .toList();
    }

    private boolean memeGroupe(CategorieObjet a, CategorieObjet b) {
        if (GROUPE_OBJETS_ET_REPAS.contains(a) && GROUPE_OBJETS_ET_REPAS.contains(b)) {
            return true;
        }
        return a == b;
    }
}