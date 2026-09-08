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
import com.loupsolitaire.backend.service.record.ResultatAjout;

@ExtendWith(MockitoExtension.class)
class InventaireServiceTest {

    @Mock
    private InventaireItemRepository inventaireItemRepository;

    @InjectMocks
    private InventaireService inventaireService;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
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
    // Ajout simple (pas de plafonnement)
    // =========================================================

    @Test
    void ajouteUnNouvelObjetAvecSaQuantite() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        when(inventaireItemRepository.findByPersonnage(personnage)).thenReturn(List.of());
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "hache")).thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, hache, 1);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(1);
        assertThat(resultat.estPlafonne()).isFalse();
        assertThat(resultat.objetsRemplacables()).isEmpty();
    }

    @Test
    void incrementeLaQuantiteSiLObjetEstDejaPossede() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        InventaireItem ligneExistante = creerLigne(repas, 2);

        when(inventaireItemRepository.findByPersonnage(personnage)).thenReturn(List.of(ligneExistante));
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "repas"))
                .thenReturn(Optional.of(ligneExistante));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, repas, 3);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(3);
        assertThat(ligneExistante.getQuantite()).isEqualTo(5);
    }

    @Test
    void nAppliqueAucuneLimitePourLesObjetsSpeciaux() {
        Objet carte = creerObjet("carte", CategorieObjet.OBJETS_SPECIAUX);
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "carte")).thenReturn(Optional.empty());
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
    void plafonneUneTroisiemeArmeAZeroEtProposeLesArmesActuelles() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        Objet glaive = creerObjet("glaive", CategorieObjet.ARME);
        Objet lance = creerObjet("lance", CategorieObjet.ARME);
        InventaireItem ligneHache = creerLigne(hache, 1);
        InventaireItem ligneGlaive = creerLigne(glaive, 1);

        when(inventaireItemRepository.findByPersonnage(personnage))
                .thenReturn(List.of(ligneHache, ligneGlaive));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, lance, 1);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(0);
        assertThat(resultat.estPlafonne()).isTrue();
        assertThat(resultat.objetsRemplacables()).containsExactlyInAnyOrder(ligneHache, ligneGlaive);
        verify(inventaireItemRepository, never()).save(any());
    }

    @Test
    void plafonneObjetEtRepasCumulesALaPlaceRestante() {
        Objet repas = creerObjet("repas", CategorieObjet.REPAS);
        Objet laumspur = creerObjet("laumspur", CategorieObjet.OBJET);

        // 6 deja en inventaire (categories melangees), il ne reste que 2 places sur 8.
        when(inventaireItemRepository.findByPersonnage(personnage))
                .thenReturn(List.of(creerLigne(repas, 4), creerLigne(laumspur, 2)));
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "laumspur"))
                .thenReturn(Optional.of(creerLigne(laumspur, 2)));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, laumspur, 3);

        assertThat(resultat.quantiteDemandee()).isEqualTo(3);
        assertThat(resultat.quantiteAjoutee()).isEqualTo(2);
        assertThat(resultat.estPlafonne()).isTrue();
    }

    @Test
    void plafonneLaBourseEnComptantOrEtPierresPrecieusesEnsemble() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        Objet pierrePrecieuse = creerObjet("pierre_precieuse", CategorieObjet.BOURSE);

        // 45 deja en bourse (30 or + 15 pierres), il ne reste que 5 places sur 50.
        when(inventaireItemRepository.findByPersonnage(personnage))
                .thenReturn(List.of(creerLigne(or, 30), creerLigne(pierrePrecieuse, 15)));

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, or, 10);

        assertThat(resultat.quantiteAjoutee()).isEqualTo(5);
        assertThat(resultat.estPlafonne()).isTrue();
    }

    // =========================================================
    // Retrait (valeur negative dans ObjetChap : paiement, objet detruit)
    // =========================================================

    @Test
    void retireUneQuantitePartielleSansSupprimerLaLigne() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        InventaireItem ligne = creerLigne(or, 10);
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "or"))
                .thenReturn(Optional.of(ligne));
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventaireService.retirerObjet(personnage, or, 2);

        assertThat(ligne.getQuantite()).isEqualTo(8);
    }

    @Test
    void supprimeLaLigneQuandLaQuantiteTombeAZero() {
        Objet pierreDeVordak = creerObjet("pierre_de_vordak", CategorieObjet.OBJET);
        InventaireItem ligne = creerLigne(pierreDeVordak, 1);
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "pierre_de_vordak"))
                .thenReturn(Optional.of(ligne));

        inventaireService.retirerObjet(personnage, pierreDeVordak, 1);

        verify(inventaireItemRepository).delete(ligne);
        verify(inventaireItemRepository, never()).save(any());
    }

    @Test
    void refuseDeRetirerUnObjetNonPossede() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "or")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventaireService.retirerObjet(personnage, or, 5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseDeRetirerPlusQueLaQuantitePossedee() {
        Objet or = creerObjet("or", CategorieObjet.BOURSE);
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "or"))
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
    // Remplacement (retirer une arme possedee pour en prendre une nouvelle)
    // =========================================================

    @Test
    void remplacerObjetRetireLAncienEtAjouteLeNouveau() {
        Objet hache = creerObjet("hache", CategorieObjet.ARME);
        Objet lance = creerObjet("lance", CategorieObjet.ARME);
        InventaireItem ligneHache = creerLigne(hache, 1);

        // Retrait de la hache : elle disparait de l'inventaire (quantite -> 0).
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "hache"))
                .thenReturn(Optional.of(ligneHache));

        // Au moment d'ajouter la lance, la hache a deja ete retiree : une
        // seule arme restante ("glaive"), donc de la place pour la lance.
        Objet glaive = creerObjet("glaive", CategorieObjet.ARME);
        when(inventaireItemRepository.findByPersonnage(personnage))
                .thenReturn(List.of(creerLigne(glaive, 1)));
        when(inventaireItemRepository.findByPersonnageAndObjetId(personnage, "lance"))
                .thenReturn(Optional.empty());
        when(inventaireItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultatAjout resultat = inventaireService.remplacerObjet(personnage, hache, 1, lance, 1);

        verify(inventaireItemRepository).delete(ligneHache);
        assertThat(resultat.quantiteAjoutee()).isEqualTo(1);
        assertThat(resultat.estPlafonne()).isFalse();
    }
}