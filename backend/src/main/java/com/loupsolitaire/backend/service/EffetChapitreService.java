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
import com.loupsolitaire.backend.model.enums.StatutRepas;
import com.loupsolitaire.backend.model.enums.TypeCondition;

import lombok.RequiredArgsConstructor;

// Applique les effets de Chapitre (distincts des effets d'Objet, geres par
// ObjetService). REPAS, HABILITE, ENDURANCE, VOL et MORT geres.
//
// Le Personnage recu est suivi par la transaction en cours (charge par
// PartieService) : ses modifications sont enregistrees automatiquement a la
// validation, sans save() explicite. Seules les NOUVELLES entites sont
// enregistrees avec save().
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
    //   11 -> le Sac a Dos lui-meme (donc son contenu) + les armes ;
    //   10 -> armes + contenu du Sac, le Sac reste ; 9 -> le Sac a Dos
    //   lui-meme ; 8 -> contenu du Sac ; 2+cond ARME -> toutes les armes ;
    //   1+cond ARME -> 1 arme au choix ; 1 sans condition -> 1 objet/repas/
    //   arme au choix.
    private static final int VOL_SAC_A_DOS_ET_ARMES = 11;
    private static final int VOL_TOUT = 10;
    private static final int VOL_SAC_A_DOS = 9;
    private static final int VOL_SAC = 8;
    private static final int VOL_TOUTES_ARMES = 2;
    private static final int VOL_UN_AU_CHOIX = 1;
    private static final Set<CategorieObjet> CATEGORIES_SAC = Set.of(CategorieObjet.OBJET, CategorieObjet.REPAS);
    private static final Set<CategorieObjet> CATEGORIES_TOUT =
            Set.of(CategorieObjet.OBJET, CategorieObjet.REPAS, CategorieObjet.ARME);

    private final InventaireService inventaireService;
    private final ConditionService conditionService;

    // Regle du Repas : la Discipline Kai de la Chasse en dispense
    // completement (le personnage se debrouille pour trouver a manger).
    // Sinon, on consomme 1 Repas de l'inventaire si possible ; a defaut,
    // -3 ENDURANCE.
    @Transactional
    public void appliquerEffetRepas(Personnage personnage) {
        if (personnage.possedeDiscipline(IdDiscipline.CHASSE)) {
            personnage.setDernierStatutRepas(StatutRepas.CHASSE);
            return;
        }

        if (inventaireService.retirerUnSiPossede(personnage, OBJET_ID_REPAS)) {
            personnage.setDernierStatutRepas(StatutRepas.REPAS_CONSOMME);
        } else {
            int nouvelleEndurance = Math.max(0, personnage.getEnduranceActuelle() + MALUS_SANS_REPAS);
            personnage.setEnduranceActuelle(nouvelleEndurance);
            if (nouvelleEndurance <= 0) {
                personnage.marquerMort();
            }
            personnage.setDernierStatutRepas(StatutRepas.MALUS_ENDURANCE);
        }
    }

    // Regle HABILITE, selon la (au plus une) condition de l'effet :
    // - Aucune condition : applique directement a habiliteTemp.
    // - DISCIPLINE / OBJET (semantique INVERSEE, valeur negative dans les
    //   donnees : "si vous NE possedez PAS") : applique a habiliteTemp
    //   uniquement si la discipline/l'objet est absent.
    // - PERMANENT : applique a habiliteBase (definitif), puis recalcule
    //   l'HABILITE effective (qui depend aussi des armes possedees).
    // - Le reste (ASSAUT_MAX, ASSAUT_MIN, etc., meme avec une protection
    //   DISCIPLINE comme au 283) : ignore ici, CombatService l'applique
    //   assaut par assaut.
    //   Dont SANS_ARME (REGLE-11, 260) : rien a l'arrivee, CombatService
    //   l'applique pendant le combat a la place de l'ajustement d'arme.
    @Transactional
    public void appliquerEffetHabilite(Personnage personnage, Effet effet) {
        List<Cond> conditions = effet.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            appliquerHabiliteTemp(personnage, effet.getValeur());
            return;
        }

        /*
         * Cas PERMANENT.
         *
         * Dans les donnees actuelles, PERMANENT est utilise seul.
         * On le traite avant les protections OBJET/DISCIPLINE.
         */
        if (conditions.size() == 1
                && conditions.getFirst().getType() == TypeCondition.PERMANENT) {

            appliquerHabilitePermanent(
                    personnage,
                    effet.getValeur()
            );

            return;
        }

        // Uniquement des protections OBJET/DISCIPLINE : le malus s'applique
        // (une seule fois) s'il en manque au moins une.
        if (conditions.stream().allMatch(ConditionService::estProtection)) {
            if (!conditionService.protectionsToutesPossedees(personnage, conditions)) {
                appliquerHabiliteTemp(personnage, effet.getValeur());
            }
            return;
        }
        /*
         * Les autres conditions ne sont pas appliquees ici.
         *
         * ASSAUT_MAX / ASSAUT_MIN, par exemple, sont geres dynamiquement dans
         * CombatService en fonction du nombre d'assauts deja livres.
         */
    }

    // Regle ENDURANCE : toujours REEL (jamais temporaire, contrairement a
    // HABILITE). Dans ce tome, la seule condition rencontree sur un effet
    // ENDURANCE est HASARD (jamais discipline/objet) : pas de semantique
    // inversee a gerer ici, on reutilise directement ConditionService.
    // Toute autre condition (conditions de combat, gerees par CombatService)
    // fait qu'on ignore l'effet, comme pour HABILITE.
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
        if (nouvelleEndurance <= 0) {
            personnage.marquerMort();
        }
    }

    // Regle MORT : mort narrative (ex. chapitre 53, 108, 127...), distincte
    // de la mort par perte d'endurance (appliquerEffetEndurance /
    // appliquerEffetRepas). Meme convention que pour ENDURANCE : la seule
    // condition rencontree dans ce tome est HASARD (ex. chapitre 2102,
    // "si vous n'obtenez pas 9") ; toute autre condition (combat non
    // construit) fait qu'on ignore l'effet. La valeur elle-meme (toujours
    // 1 dans les donnees) n'est qu'un marqueur, pas une quantite.
    // On remet aussi l'ENDURANCE a 0 par coherence avec le reste du
    // systeme (Personnage.mort suppose une endurance nulle), meme si la
    // mort narrative n'en decoule pas directement dans le recit.
    @Transactional
    public void appliquerEffetMort(Personnage personnage, Effet effet) {
        Optional<Cond> condition = effet.getConditions().stream().findFirst();

        if (condition.isPresent()) {
            if (condition.get().getType() != TypeCondition.HASARD) {
                return;
            }
            if (!conditionService.estDisponible(condition.get(), personnage)) {
                return;
            }
        }

        personnage.setEnduranceActuelle(0);
        personnage.marquerMort();
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

        if (effet.getValeur() == VOL_SAC_A_DOS_ET_ARMES) {
            perdreSacADos(personnage);
            retirerTout(personnage, Set.of(CategorieObjet.ARME));
        } else if (effet.getValeur() == VOL_SAC_A_DOS) {
            perdreSacADos(personnage);
        } else if (effet.getValeur() == VOL_TOUT) {
            retirerTout(personnage, CATEGORIES_TOUT);
        } else if (effet.getValeur() == VOL_SAC) {
            retirerTout(personnage, CATEGORIES_SAC);
        } else if (effet.getValeur() == VOL_TOUTES_ARMES && gateArme) {
            retirerTout(personnage, Set.of(CategorieObjet.ARME));
        } else if (effet.getValeur() == VOL_UN_AU_CHOIX) {
            mettreVolAuChoixEnAttente(personnage, gateArme);
        }
        // Toute autre valeur : non rencontree dans ce tome, on ignore.
    }

    private void mettreVolAuChoixEnAttente(Personnage personnage, boolean gateArme) {
        Set<CategorieObjet> categoriesEligibles = gateArme ? Set.of(CategorieObjet.ARME) : CATEGORIES_TOUT;
        boolean aQuelqueChoseAPerdre = inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> categoriesEligibles.contains(item.getObjet().getCategorie()));

        if (aQuelqueChoseAPerdre) {
            personnage.setVolEnAttente(gateArme ? PorteeVol.ARME : PorteeVol.TOUT);
        }
        // Sinon : rien a voler, on ne bloque pas la partie pour un vol
        // impossible a resoudre.
    }

    // Resout un vol en attente (Effet VOL, valeur=1) : le joueur a choisi
    // quel objet perdre. Verifie que le choix respecte la portee autorisee
    // avant de retirer l'objet et de lever l'attente.
    @Transactional
    public void resoudreVolEnAttente(Personnage personnage, Objet objet) {
        personnage.verifierVivant();
        PorteeVol portee = personnage.getVolEnAttente();
        if (portee == null) {
            // 400 et non 500 : declenchable par le joueur (POST /vol/{objetId}
            // alors qu'aucun vol n'est en attente, ex. double envoi).
            throw new IllegalArgumentException("Aucun vol en attente pour ce personnage");
        }

        boolean categorieValide = portee == PorteeVol.ARME
                ? objet.getCategorie() == CategorieObjet.ARME
                : CATEGORIES_TOUT.contains(objet.getCategorie());
        if (!categorieValide) {
            throw new IllegalArgumentException(
                    "Cet objet ne correspond pas a la portee du vol en attente (" + portee + ")");
        }
        // REGLE-09 (144) : le voleur prend dans le Sac ; une arme seulement si
        // le Sac ne contient ni objet ni repas.
        if (portee == PorteeVol.TOUT && objet.getCategorie() == CategorieObjet.ARME && sacContientQuelqueChose(personnage)) {
            throw new IllegalArgumentException(
                    "Le voleur prend un objet de votre Sac a Dos : choisissez un objet ou un repas");
        }

        inventaireService.retirerObjet(personnage, objet, 1);
        personnage.setVolEnAttente(null);
    }

    private boolean sacContientQuelqueChose(Personnage personnage) {
        return inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> CATEGORIES_SAC.contains(item.getObjet().getCategorie()) && item.getQuantite() > 0);
    }

    // Retire integralement tous les objets possedes dans les categories
    // donnees (utilise par VOL_TOUT/VOL_SAC/VOL_TOUTES_ARMES).
    // Retirer le Sac vide aussi son contenu (InventaireService.retirerObjet).
    private void perdreSacADos(Personnage personnage) {
        inventaireService.listerInventaire(personnage).stream()
                .filter(item -> InventaireService.OBJET_SAC_A_DOS.equals(item.getObjet().getId()))
                .findFirst()
                .ifPresent(item -> inventaireService.retirerObjet(personnage, item.getObjet(), item.getQuantite()));
    }

    private void retirerTout(Personnage personnage, Set<CategorieObjet> categories) {
        List<InventaireItem> items = inventaireService.listerInventaire(personnage).stream()
                .filter(item -> categories.contains(item.getObjet().getCategorie()))
                .toList();

        for (InventaireItem item : items) {
            inventaireService.retirerObjet(personnage, item.getObjet(), item.getQuantite());
        }
    }

    private void appliquerHabiliteTemp(Personnage personnage, int valeur) {
        personnage.setHabiliteTemp(personnage.getHabiliteTemp() + valeur);
    }

    private void appliquerHabilitePermanent(Personnage personnage, int valeur) {
        personnage.setHabiliteBase(personnage.getHabiliteBase() + valeur);
        // habilite depend de habiliteBase + etat des armes : recalcul
        // necessaire pour que le changement permanent soit reellement
        // reflete dans la valeur effective.
        inventaireService.recalculerHabiliteArmes(personnage);
    }
}