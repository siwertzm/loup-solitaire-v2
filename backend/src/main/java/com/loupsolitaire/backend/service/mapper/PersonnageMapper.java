package com.loupsolitaire.backend.service.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.response.InventaireItemResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.InventaireService;
import com.loupsolitaire.backend.util.Textes;

import lombok.RequiredArgsConstructor;

// Mapping partage Personnage -> PersonnageResponse, utilise par
// PersonnageController (endpoints /personnages) et AuthController (/auth/me,
// pour afficher la liste des personnages jouables sur l'ecran profil).
@Component
@RequiredArgsConstructor
public class PersonnageMapper {

    private final InventaireService inventaireService;

    public PersonnageResponse versReponse(Personnage personnage) {
        return versReponse(personnage, inventaireService.listerInventaire(personnage));
    }

    // OPS-07 : liste de personnages, inventaires lus en une seule requete.
    public List<PersonnageResponse> versReponses(List<Personnage> personnages) {
        Map<UUID, List<InventaireItem>> inventaires = inventaireService.listerInventaires(personnages);
        return personnages.stream()
                .map(p -> versReponse(p, inventaires.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private PersonnageResponse versReponse(Personnage personnage, List<InventaireItem> items) {
        List<InventaireItemResponse> inventaire = items.stream()
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
                personnage.getArmeMaitrisee() != null
                        ? Textes.selon(personnage.getArmeMaitrisee().getNom(), personnage.getArmeMaitrisee().getNomEn())
                        : null,
                personnage.getChapitreActuel().getId(),
                personnage.getVolEnAttente() != null ? personnage.getVolEnAttente().name() : null,
                personnage.isMort(),
                inventaire
        );
    }

    private InventaireItemResponse versReponseInventaire(InventaireItem item) {
        return new InventaireItemResponse(
                item.getObjet().getId(),
                Textes.selon(item.getObjet().getNom(), item.getObjet().getNomEn()),
                item.getObjet().getCategorie().name(),
                item.getQuantite()
        );
    }
}