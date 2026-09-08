package com.loupsolitaire.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.PersonnageRepository;

import lombok.RequiredArgsConstructor;

// Applique les effets de Chapitre (distincts des effets d'Objet, geres par
// ObjetService). Commence par REPAS ; ENDURANCE/HABILETE viendront ensuite.
@Service
@RequiredArgsConstructor
public class EffetChapitreService {

    // Valeur trouvee dans le texte narratif (ex. chapitre 147), jamais dans
    // les donnees structurees : voir doc de conception, limitation connue.
    private static final int MALUS_SANS_REPAS = -3;
    private static final String OBJET_ID_REPAS = "repas";

    private final InventaireService inventaireService;
    private final PersonnageRepository personnageRepository;

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
}