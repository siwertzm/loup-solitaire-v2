package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.PersonnageRepository;

@ExtendWith(MockitoExtension.class)
class EffetChapitreServiceTest {

    @Mock
    private InventaireService inventaireService;
    @Mock
    private PersonnageRepository personnageRepository;

    @InjectMocks
    private EffetChapitreService effetChapitreService;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
        personnage.setEnduranceActuelle(15);
    }

    private InventaireItem creerLigneRepas(int quantite) {
        Objet repas = new Objet();
        repas.setId("repas");
        repas.setCategorie(CategorieObjet.REPAS);
        InventaireItem item = new InventaireItem();
        item.setObjet(repas);
        item.setQuantite(quantite);
        return item;
    }

    @Test
    void consommeUnRepasSiLePersonnageEnPossedeSansToucherALEndurance() {
        InventaireItem ligneRepas = creerLigneRepas(2);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(ligneRepas));

        effetChapitreService.appliquerEffetRepas(personnage);

        verify(inventaireService).retirerObjet(personnage, ligneRepas.getObjet(), 1);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15); // inchangee
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void appliqueMoins3EnduranceSiAucunRepasDisponible() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(12); // 15 - 3
        verify(inventaireService, never()).retirerObjet(any(), any(), eq(1));
        verify(personnageRepository).save(personnage);
    }

    @Test
    void neDescendJamaisEnDessousDeZero() {
        personnage.setEnduranceActuelle(1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(0);
    }

    @Test
    void laDisciplineChasseDispenseCompletementDeLaRegle() {
        Discipline chasse = new Discipline();
        chasse.setId(IdDiscipline.CHASSE);
        personnage.setDisciplines(List.of(chasse));

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15); // inchangee
        verify(inventaireService, never()).listerInventaire(any());
        verify(inventaireService, never()).retirerObjet(any(), any(), anyInt());
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void neConsommeQuUnSeulRepasMemeSiPlusieursPossedes() {
        InventaireItem ligneRepas = creerLigneRepas(5);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(ligneRepas));

        effetChapitreService.appliquerEffetRepas(personnage);

        verify(inventaireService).retirerObjet(personnage, ligneRepas.getObjet(), 1);
    }
}