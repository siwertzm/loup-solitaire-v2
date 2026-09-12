package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.repository.InventaireItemRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ResultatAjout;

@ExtendWith(MockitoExtension.class)
class InventaireServiceTest {

    @Mock
    private InventaireItemRepository inventaireItemRepository;
    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private ObjetService objetService;

    @InjectMocks
    private InventaireService inventaireService;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
        personnage.setHabiliteBase(15);
        personnage.setHabilite(15);
    }

    private Objet creerObjet(String id, CategorieObjet categorie) {
        Objet objet = new Objet();
        objet.setId(id);
        objet.setCategorie(categorie);
        return objet;
    }

    private InventaireItem creerLigne(Objet objet, int quantite) {
        InventaireItem item = new InventaireItem();
        item.setPersonnage(personnage);
        item.setObjet(objet);
        item.setQuantite(quantite);
        return item;
    }

    // =========================================================
    // Ajout simple
    // =========================================================

    @Test
    void ajouteUnNouvelObjetAvecSaQuantiteEtAppliqueSesBonus() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "repas")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, repas, 1);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(1);
        assertThat(resultat.estPlafonne()).isFalse();
        assertThat(resultat.objetsRemplacables()).isEmpty();
        verify(objetService).appliquerBonusRecuperation(personnage, repas);
        // Pas une arme : pas de recalcul d'habilite.
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void incrementeLaQuantiteSiLObjetEstDejaPossede() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        InventaireItem ligneExistante = creerLigne(repas, 2);

        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "repas"))
                .thenReturn(Optional.of(ligneExistante));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, repas, 3);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(3);
        assertThat(ligneExistante.getQuantite()).isEqualTo(5);
    }

    @Test
    void nAppliqueAucuneLimitePourLesObjetsSpeciaux() {
        Objet carte = creerObjet("carte", CategorieObjet.OBJETS_SPECIAUX);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "carte")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, carte, 1);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(1);
        assertThat(resultat.estPlafonne()).isFalse();
    }

    @Test
    void refuseUneQuantiteNulleOuNegative() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);

        assertThatThrownBy(() -> inventaireService.ajouterObjet(personnage, or, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventaireService.ajouterObjet(personnage, or, -5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================
    // Plafonnement (jamais d'exception, on ajoute ce qui rentre)
    // =========================================================

    @Test
    void plafonneUneTroisiemeArmeAZeroEtProposeLesArmesActuellesSansAppelerObjetService() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        Objet glaive = creerObjet("glaive", CategorieObjet.ARME);
        Objet lance = creerObjet("lance", CategorieObjet.ARME);
        InventaireItem ligneHache = creerLigne(hache, 1);
        InventaireItem ligneGlaive = creerLigne(glaive, 1);

        when(inventaireItemRepository.findByPersonnageId(personnage.getId()))
                .thenReturn(List.of(ligneHache, ligneGlaive));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, lance, 1);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(0);
        assertThat(resultat.estPlafonne()).isTrue();
        assertThat(resultat.objetsRemplacables()).containsExactlyInAnyOrder(ligneHache, ligneGlaive);
        verify(inventaireItemRepository, never()).save(any());
        // Rien n'a ete ajoute : pas de bonus a appliquer, pas de recalcul.
        verify(objetService, never()).appliquerBonusRecuperation(any(), any());
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void plafonneObjetEtRepasCumulesALaPlaceRestante() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        Objet laumspur = creerObjet("laumspur", CategorieObjet.OBJET);

        when(inventaireItemRepository.findByPersonnageId(personnage.getId()))
                .thenReturn(List.of(creerLigne(repas, 4), creerLigne(laumspur, 2)));
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "laumspur"))
                .thenReturn(Optional.of(creerLigne(laumspur, 2)));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, laumspur, 3);

        assertThat(resultat.quantiteDemandee()).isEqualTo(3);
        assertThat(resultat.quantiteAjoutee()).isEqualTo(2);
        assertThat(resultat.estPlafonne()).isTrue();
        verify(objetService).appliquerBonusRecuperation(personnage, laumspur);
    }

    @Test
    void plafonneLaBourseEnComptantOrEtPierresPrecieusesEnsemble() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        Objet pierrePrecieuse = creerObjet("pierre_precieuse", CategorieObjet.BOURSE);

        when(inventaireItemRepository.findByPersonnageId(personnage.getId()))
                .thenReturn(List.of(creerLigne(or, 30), creerLigne(pierrePrecieuse, 15)));
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "or")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, or, 10);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(5);
        assertThat(resultat.estPlafonne()).isTrue();
    }

    // =========================================================
    // Retrait (valeur negative dans ObjetChap : paiement, objet detruit)
    // =========================================================

    @Test
    void retireUneQuantitePartielleSansSupprimerLaLigneNiToucherLeBonus() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        InventaireItem ligne = creerLigne(or, 10);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "or"))
                .thenReturn(Optional.of(ligne));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventaireService.retirerObjet(personnage, or, 2);

        assertThat(ligne.getQuantite()).isEqualTo(8);
        verify(objetService, never()).retirerBonusPerte(any(), any());
        verify(personnageRepository, never()).save(any()); // pas une arme
    }

    @Test
    void supprimeLaLigneQuandLaQuantiteTombeAZeroEtRetireLeBonus() {
        Objet casque = creerObjet("casque", CategorieObjet.OBJETS_SPECIAUX);
        InventaireItem ligne = creerLigne(casque, 1);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "casque"))
                .thenReturn(Optional.of(ligne));

        inventaireService.retirerObjet(personnage, casque, 1);

        verify(inventaireItemRepository).delete(ligne);
        verify(inventaireItemRepository, never()).save(any());
        verify(objetService).retirerBonusPerte(personnage, casque);
    }

    @Test
    void refuseDeRetirerUnObjetNonPossede() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "or")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventaireService.retirerObjet(personnage, or, 5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseDeRetirerPlusQueLaQuantitePossedee() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "or"))
                .thenReturn(Optional.of(creerLigne(or, 3)));

        assertThatThrownBy(() -> inventaireService.retirerObjet(personnage, or, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseUneQuantiteDeRetraitNulleOuNegative() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);

        assertThatThrownBy(() -> inventaireService.retirerObjet(personnage, or, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventaireService.retirerObjet(personnage, or, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================
    // Recalcul d'HABILETE base sur les armes (etat global, pas un delta)
    // =========================================================

    @Test
    void appliqueMoins4QuandLaDerniereArmeEstRetiree() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        InventaireItem ligne = creerLigne(hache, 1);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "hache"))
                .thenReturn(Optional.of(ligne));
        // Apres suppression de la ligne, l'inventaire ne contient plus d'arme.
        when(inventaireItemRepository.findByPersonnageId(personnage.getId())).thenReturn(List.of());

        inventaireService.retirerObjet(personnage, hache, 1);

        assertThat(personnage.getHabilite()).isEqualTo(11); // 15 - 4
        verify(personnageRepository).save(personnage);
    }

    @Test
    void nAppliqueAucunAjustementAvecUneArmeNonMaitrisee() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "hache")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventaireItemRepository.findByPersonnageId(personnage.getId())).thenReturn(List.of(creerLigne(hache, 1)));

        inventaireService.ajouterObjet(personnage, hache, 1);

        assertThat(personnage.getHabilite()).isEqualTo(15); // inchange, sans malus ni bonus
        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliquePlus2QuandLArmeMaitriseeEstAjoutee() {
        Objet epee = creerObjet("epee", CategorieObjet.ARME);
        personnage.setArmeMaitrisee(epee);
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "epee")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventaireItemRepository.findByPersonnageId(personnage.getId())).thenReturn(List.of(creerLigne(epee, 1)));

        inventaireService.ajouterObjet(personnage, epee, 1);

        assertThat(personnage.getHabilite()).isEqualTo(17); // 15 + 2
    }

    @Test
    void neRecalculeRienPourUnObjetNonArme() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        when(inventaireItemRepository.findByPersonnageId(personnage.getId())).thenReturn(List.of());
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "repas")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventaireService.ajouterObjet(personnage, repas, 1);

        assertThat(personnage.getHabilite()).isEqualTo(15);
        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // Remplacement (retirer une arme possedee pour en prendre une nouvelle)
    // =========================================================

    @Test
    void remplacerObjetRetireLAncienEtAjouteLeNouveau() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        Objet lance = creerObjet("lance", CategorieObjet.ARME);
        InventaireItem ligneHache = creerLigne(hache, 1);
        Objet glaive = creerObjet("glaive", CategorieObjet.ARME);

        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "hache"))
                .thenReturn(Optional.of(ligneHache));
        when(inventaireItemRepository.findByPersonnageId(personnage.getId()))
                .thenReturn(List.of(creerLigne(glaive, 1)));
        when(inventaireItemRepository.findByPersonnageIdAndObjetId(personnage.getId(), "lance"))
                .thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.remplacerObjet(personnage, hache, 1, lance, 1);

        verify(inventaireItemRepository).delete(ligneHache);
        assertThat(resultat.quantiteAjoutee()).isEqualTo(1);
        assertThat(resultat.estPlafonne()).isFalse();
        verify(objetService).appliquerBonusRecuperation(personnage, lance);
    }
}