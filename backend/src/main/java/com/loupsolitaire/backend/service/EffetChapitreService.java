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
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets de Chapitre (distincts des effets d'Objet, geres par
// ObjetService). REPAS, HABILITE, ENDURANCE, VOL et MORT geres.
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
            personnage.setDernierStatutRepas(StatutRepas.CHASSE);
            personnageRepository.save(personnage);
            return;
        }

        Optional<InventaireItem> repas = inventaireService.listerInventaire(personnage).stream()
                .filter(item -> item.getObjet().getId().equals(OBJET_ID_REPAS) && item.getQuantite() > 0)
                .findFirst();

        if (repas.isPresent()) {
            inventaireService.retirerObjet(personnage, repas.get().getObjet(), 1);
            personnage.setDernierStatutRepas(StatutRepas.REPAS_CONSOMME);
            personnageRepository.save(personnage);
        } else {
            int nouvelleEndurance = Math.max(0, personnage.getEnduranceActuelle() + MALUS_SANS_REPAS);
            personnage.setEnduranceActuelle(nouvelleEndurance);
            if (nouvelleEndurance <= 0) {
                personnage.setMort(true);
            }
            personnage.setDernierStatutRepas(StatutRepas.MALUS_ENDURANCE);
            personnageRepository.save(personnage);
        }
    }

    // Regle HABILITE, selon la (au plus une) condition de l'effet :
    // - Aucune condition : applique directement a habiliteTemp.
    // - DISCIPLINE / OBJET (semantique INVERSEE, valeur negative dans les
    //   donnees : "si vous NE possedez PAS") : applique a habiliteTemp
    //   uniquement si la discipline/l'objet est absent.
    // - PERMANENT : applique a habiliteBase (definitif), puis recalcule
    //   l'HABILITE effective (qui depend aussi des armes possedees).
    // - Le reste (ASSAUT_MAX, etc.) : combat non construit, ignore.
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
                && conditions.get(0).getType() == TypeCondition.PERMANENT) {

            appliquerHabilitePermanent(
                    personnage,
                    effet.getValeur()
            );

            return;
        }

        /*
         * Conditions de protection.
         *
         * Toutes les conditions doivent etre de type OBJET ou DISCIPLINE.
         * Si c'est le cas, le joueur doit posseder TOUTES les protections
         * pour eviter le malus.
         */
        boolean uniquementConditionsProtection =
                conditions.stream()
                        .allMatch(condition ->
                                condition.getType() == TypeCondition.OBJET
                                        || condition.getType()
                                                == TypeCondition.DISCIPLINE
                        );

        if (uniquementConditionsProtection) {

            boolean toutesProtectionsPossedees =
                    conditions.stream()
                            .allMatch(condition -> {

                                if (condition.getType()
                                        == TypeCondition.OBJET) {

                                    return possedeObjet(
                                            personnage,
                                            condition.getTargetId()
                                    );
                                }

                                if (condition.getType()
                                        == TypeCondition.DISCIPLINE) {

                                    return possedeDiscipline(
                                            personnage,
                                            condition.getTargetId()
                                    );
                                }

                                return false;
                            });

            /*
             * Au moins une protection manque :
             * on applique le malus UNE seule fois.
             */
            if (!toutesProtectionsPossedees) {

                appliquerHabiliteTemp(
                        personnage,
                        effet.getValeur()
                );
            }

            return;
        }
        /*
         * Les autres conditions ne sont pas appliquees ici.
         *
         * ASSAUT_MAX, par exemple, est gere dynamiquement dans
         * CombatService en fonction du nombre d'assauts deja livres.
         */
    }

    // Regle ENDURANCE : toujours REEL (jamais temporaire, contrairement a
    // HABILITE). Dans ce tome, la seule condition rencontree sur un effet
    // ENDURANCE est HASARD (jamais discipline/objet) : pas de semantique
    // inversee a gerer ici, on reutilise directement ConditionService.
    // Toute autre condition (combat non construit) fait qu'on ignore
    // l'effet, comme pour HABILITE.
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
            personnage.setMort(true);
        }
        personnageRepository.save(personnage);
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
        personnage.setMort(true);
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
        if (personnage.isMort()) {
            throw new IllegalArgumentException(
                    "Ce personnage est mort (perte d'endurance) : ressuscitez-le via "
                            + "POST /personnages/{id}/ressusciter avant de continuer");
        }
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