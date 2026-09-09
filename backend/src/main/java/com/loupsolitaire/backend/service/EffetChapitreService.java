package com.loupsolitaire.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.PorteeVol;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets de Chapitre (distincts des effets d'Objet, geres par
// ObjetService). REPAS, HABILETE, ENDURANCE et VOL geres.
@Service
@RequiredArgsConstructor
public class EffetChapitreService {

    // Valeur trouvee dans le texte narratif (ex. chapitre 147), jamais dans
    // les donnees structurees : voir doc de conception, limitation connue.
    private static final int MALUS_SANS_REPAS = -3;
    private static final String OBJET_ID_REPAS = "repas";

    // VOL : le "valeur" code une PORTEE de perte, pas une quantite (voir
    // doc de conception, analyse menee avec l'utilisateur sur les 9 cas
    // reels du tome) :
    //   10 -> tout (sac + armes) ; 8 -> tout le sac ; 2+cond ARME -> toutes
    //   les armes ; 1+cond ARME -> 1 arme au choix ; 1 sans condition ->
    //   1 objet/repas/arme au choix.
    private static final int VOL_TOUT = 10;
    private static final int VOL_SAC = 8;
    private static final int VOL_TOUTES_ARMES = 2;
    private static final int VOL_UN_AU_CHOIX = 1;
    private static final Set<CategorieObjet> CATEGORIES_SAC = Set.of(CategorieObjet.OBJET, CategorieObjet.REPAS);
    private static final Set<CategorieObjet> CATEGORIES_TOUT =
            Set.of(CategorieObjet.OBJET, CategorieObjet.REPAS, CategorieObjet.ARME);

    private final InventaireService inventaireService;
    private final PersonnageRepository personnageRepository;
    private final ConditionService conditionService;

    // Regle du Repas : la Discipline Kai de la Chasse en dispense
    // completement (le personnage se debrouille pour trouver a manger).
    // Sinon, on consomme 1 Repas de l'inventaire si possible ; a defaut,
    // -3 ENDURANCE.
    @Transactional
    public void appliquerEffetRepas(Personnage personnage) {
        boolean possedeChasse = personnage.getDisciplines().stream()
                .anyMatch(d -> d.getId() == IdDiscipline.CHASSE);
        if (possedeChasse) {
            return;
        }

        Optional<InventaireItem> repas = inventaireService.listerInventaire(personnage).stream()
                .filter(item -> item.getObjet().getId().equals(OBJET_ID_REPAS) && item.getQuantite() > 0)
                .findFirst();

        if (repas.isPresent()) {
            inventaireService.retirerObjet(personnage, repas.get().getObjet(), 1);
        } else {
            int nouvelleEndurance = Math.max(0, personnage.getEnduranceActuelle() + MALUS_SANS_REPAS);
            personnage.setEnduranceActuelle(nouvelleEndurance);
            personnageRepository.save(personnage);
        }
    }

    // Regle HABILETE, selon la (au plus une) condition de l'effet :
    // - Aucune condition : applique directement a habiliteTemp.
    // - DISCIPLINE / OBJET (semantique INVERSEE, valeur negative dans les
    //   donnees : "si vous NE possedez PAS") : applique a habiliteTemp
    //   uniquement si la discipline/l'objet est absent.
    // - PERMANENT : applique a habiliteBase (definitif), puis recalcule
    //   l'HABILETE effective (qui depend aussi des armes possedees).
    // - Le reste (ASSAUT_MAX, etc.) : combat non construit, ignore.
    @Transactional
    public void appliquerEffetHabilite(Personnage personnage, Effet effet) {
        Optional<Cond> condition = effet.getConditions().stream().findFirst();

        if (condition.isEmpty()) {
            appliquerHabiliteTemp(personnage, effet.getValeur());
            return;
        }

        switch (condition.get().getType()) {
            case PERMANENT -> appliquerHabilitePermanent(personnage, effet.getValeur());
            case DISCIPLINE -> {
                if (!possedeDiscipline(personnage, condition.get().getTargetId())) {
                    appliquerHabiliteTemp(personnage, effet.getValeur());
                }
            }
            case OBJET -> {
                if (!possedeObjet(personnage, condition.get().getTargetId())) {
                    appliquerHabiliteTemp(personnage, effet.getValeur());
                }
            }
            default -> {
                // ASSAUT_MAX, ASSAUT_ECHEC, ENDURANCE_PERDUE, FUITE, HASARD,
                // ARME, BOURSE, ENDURANCE : combat non construit, ou non
                // rencontre sur un effet HABILETE dans ce tome. Ignore.
            }
        }
    }

    // Regle ENDURANCE : toujours REEL (jamais temporaire, contrairement a
    // HABILETE). Dans ce tome, la seule condition rencontree sur un effet
    // ENDURANCE est HASARD (jamais discipline/objet) : pas de semantique
    // inversee a gerer ici, on reutilise directement ConditionService.
    // Toute autre condition (combat non construit) fait qu'on ignore
    // l'effet, comme pour HABILETE.
    @Transactional
    public void appliquerEffetEndurance(Personnage personnage, Effet effet) {
        Optional<Cond> condition = effet.getConditions().stream().findFirst();

        if (condition.isPresent()) {
            if (condition.get().getType() != TypeCondition.HASARD) {
                return;
            }
            if (!conditionService.estDisponible(condition.get(), personnage)) {
                return;
            }
        }

        int nouvelleEndurance = personnage.getEnduranceActuelle() + effet.getValeur();
        nouvelleEndurance = Math.max(0, Math.min(nouvelleEndurance, personnage.getEnduranceMax()));
        personnage.setEnduranceActuelle(nouvelleEndurance);
        personnageRepository.save(personnage);
    }

    // Regle VOL : voir les constantes VOL_* en tete de classe pour le
    // detail de chaque portee. HASARD (ex. chapitre 188) gate le vol tout
    // entier, comme pour ENDURANCE.
    @Transactional
    public void appliquerEffetVol(Personnage personnage, Effet effet) {
        Optional<Cond> hasard = effet.getConditions().stream()
                .filter(c -> c.getType() == TypeCondition.HASARD)
                .findFirst();
        if (hasard.isPresent() && !conditionService.estDisponible(hasard.get(), personnage)) {
            return;
        }

        boolean gateArme = effet.getConditions().stream().anyMatch(c -> c.getType() == TypeCondition.ARME);

        if (effet.getValeur() == VOL_TOUT) {
            retirerTout(personnage, CATEGORIES_TOUT);
        } else if (effet.getValeur() == VOL_SAC) {
            retirerTout(personnage, CATEGORIES_SAC);
        } else if (effet.getValeur() == VOL_TOUTES_ARMES && gateArme) {
            retirerTout(personnage, Set.of(CategorieObjet.ARME));
        } else if (effet.getValeur() == VOL_UN_AU_CHOIX) {
            Set<CategorieObjet> categoriesEligibles = gateArme ? Set.of(CategorieObjet.ARME) : CATEGORIES_TOUT;
            boolean aQuelqueChoseAPerdre = inventaireService.listerInventaire(personnage).stream()
                    .anyMatch(item -> categoriesEligibles.contains(item.getObjet().getCategorie()));

            if (aQuelqueChoseAPerdre) {
                personnage.setVolEnAttente(gateArme ? PorteeVol.ARME : PorteeVol.TOUT);
                personnageRepository.save(personnage);
            }
            // Sinon : rien a voler, on ne bloque pas la partie pour un vol
            // impossible a resoudre.
        }
        // Toute autre valeur : non rencontree dans ce tome, on ignore.
    }

    // Resout un vol en attente (Effet VOL, valeur=1) : le joueur a choisi
    // quel objet perdre. Verifie que le choix respecte la portee autorisee
    // avant de retirer l'objet et de lever l'attente.
    @Transactional
    public void resoudreVolEnAttente(Personnage personnage, Objet objet) {
        PorteeVol portee = personnage.getVolEnAttente();
        if (portee == null) {
            throw new IllegalStateException("Aucun vol en attente pour ce personnage");
        }

        boolean categorieValide = portee == PorteeVol.ARME
                ? objet.getCategorie() == CategorieObjet.ARME
                : CATEGORIES_TOUT.contains(objet.getCategorie());
        if (!categorieValide) {
            throw new IllegalArgumentException(
                    "Cet objet ne correspond pas a la portee du vol en attente (" + portee + ")");
        }

        inventaireService.retirerObjet(personnage, objet, 1);
        personnage.setVolEnAttente(null);
        personnageRepository.save(personnage);
    }

    // Retire integralement tous les objets possedes dans les categories
    // donnees (utilise par VOL_TOUT/VOL_SAC/VOL_TOUTES_ARMES).
    private void retirerTout(Personnage personnage, Set<CategorieObjet> categories) {
        List<InventaireItem> items = inventaireService.listerInventaire(personnage).stream()
                .filter(item -> categories.contains(item.getObjet().getCategorie()))
                .toList();

        for (InventaireItem item : items) {
            inventaireService.retirerObjet(personnage, item.getObjet(), item.getQuantite());
        }
    }

    private boolean possedeDiscipline(Personnage personnage, String targetId) {
        IdDiscipline recherchee = IdDiscipline.fromJson(targetId);
        return personnage.getDisciplines().stream().anyMatch(d -> d.getId() == recherchee);
    }

    private boolean possedeObjet(Personnage personnage, String objetId) {
        return inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> item.getObjet().getId().equals(objetId) && item.getQuantite() > 0);
    }

    private void appliquerHabiliteTemp(Personnage personnage, int valeur) {
        personnage.setHabiliteTemp(personnage.getHabiliteTemp() + valeur);
        personnageRepository.save(personnage);
    }

    private void appliquerHabilitePermanent(Personnage personnage, int valeur) {
        personnage.setHabiliteBase(personnage.getHabiliteBase() + valeur);
        personnageRepository.save(personnage);
        // habilite depend de habiliteBase + etat des armes : recalcul
        // necessaire pour que le changement permanent soit reellement
        // reflete dans la valeur effective.
        inventaireService.recalculerHabiliteArmes(personnage);
    }
}