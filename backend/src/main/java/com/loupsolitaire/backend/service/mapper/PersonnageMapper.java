package com.loupsolitaire.backend.service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.response.InventaireItemResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.InventaireService;

import lombok.RequiredArgsConstructor;

// Mapping partage Personnage -> PersonnageResponse, utilise par
// PersonnageController (endpoints /personnages) et AuthController (/auth/me,
// pour afficher la liste des personnages jouables sur l'ecran profil).
@Component
@RequiredArgsConstructor
public class PersonnageMapper {

    private final InventaireService inventaireService;

    public PersonnageResponse versReponse(Personnage personnage) {
        List<InventaireItemResponse> inventaire = inventaireService.listerInventaire(personnage).stream()
                .map(this::versReponseInventaire)
                .toList();

        return new PersonnageResponse(
                personnage.getId(),
                personnage.getNom(),
                personnage.getHabiliteBase(),
                personnage.getHabilite(),
                personnage.getHabiliteTemp(),
                personnage.getEnduranceMax(),
                personnage.getEnduranceActuelle(),
                personnage.getDisciplines().stream().map(d -> d.getId().name()).toList(),
                personnage.getArmeMaitrisee() != null ? personnage.getArmeMaitrisee().getNom() : null,
                personnage.getChapitreActuel().getId(),
                personnage.getVolEnAttente() != null ? personnage.getVolEnAttente().name() : null,
                inventaire
        );
    }

    private InventaireItemResponse versReponseInventaire(InventaireItem item) {
        return new InventaireItemResponse(
                item.getObjet().getId(),
                item.getObjet().getNom(),
                item.getObjet().getCategorie().name(),
                item.getQuantite()
        );
    }
}