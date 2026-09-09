package com.loupsolitaire.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets de Chapitre (distincts des effets d'Objet, geres par
// ObjetService). REPAS, HABILETE et ENDURANCE geres ; VOL laisse de cote
// (donnees insuffisantes, voir doc de conception).
@Service
@RequiredArgsConstructor
public class EffetChapitreService {

    // Valeur trouvee dans le texte narratif (ex. chapitre 147), jamais dans
    // les donnees structurees : voir doc de conception, limitation connue.
    private static final int MALUS_SANS_REPAS = -3;
    private static final String OBJET_ID_REPAS = "repas";

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