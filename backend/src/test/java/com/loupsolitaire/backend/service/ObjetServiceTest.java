package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.PersonnageRepository;

@ExtendWith(MockitoExtension.class)
class ObjetServiceTest {

    @Mock
    private PersonnageRepository personnageRepository;

    @InjectMocks
    private ObjetService objetService;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
        personnage.setHabilite(15);
        personnage.setHabiliteTemp(0);
        personnage.setEnduranceMax(20);
        personnage.setEnduranceActuelle(18);
    }

    private Effet creerEffet(TypeEffet type, int valeur) {
        Effet effet = new Effet();
        effet.setType(type);
        effet.setValeur(valeur);
        return effet;
    }

    private Objet creerObjet(String id, CategorieObjet categorie, Effet... effets) {
        Objet objet = new Objet();
        objet.setId(id);
        objet.setCategorie(categorie);
        objet.setEffets(List.of(effets));
        return objet;
    }

    // =========================================================
    // Armure (OBJETS_SPECIAUX) : endurance reelle, passive, touche le plafond
    // =========================================================

    @Test
    void uneArmureAugmenteLePlafondEtLEnduranceActuelle() {
        Objet casque = creerObjet("casque", CategorieObjet.OBJETS_SPECIAUX, creerEffet(TypeEffet.ENDURANCE, 2));

        objetService.appliquerBonusRecuperation(personnage, casque);

        assertThat(personnage.getEnduranceMax()).isEqualTo(22);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
        verify(personnageRepository).save(personnage);
    }

    @Test
    void perdreUneArmureRetireLePlafondEtLEnduranceActuelle() {
        personnage.setEnduranceMax(24);
        personnage.setEnduranceActuelle(24);
        Objet cotteDeMailles = creerObjet("cotte_de_mailles", CategorieObjet.OBJETS_SPECIAUX,
                creerEffet(TypeEffet.ENDURANCE, 4));

        objetService.retirerBonusPerte(personnage, cotteDeMailles);

        assertThat(personnage.getEnduranceMax()).isEqualTo(20);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
    }

    @Test
    void laPerteDUneArmureNeFaitJamaisDescendreEnduranceActuelleSousZero() {
        personnage.setEnduranceMax(4);
        personnage.setEnduranceActuelle(2);
        Objet casque = creerObjet("casque", CategorieObjet.OBJETS_SPECIAUX, creerEffet(TypeEffet.ENDURANCE, 4));

        objetService.retirerBonusPerte(personnage, casque);

        assertThat(personnage.getEnduranceMax()).isEqualTo(0);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(0);
    }

    // =========================================================
    // Consommable ENDURANCE (OBJET) : reel, applique une fois, jamais retire
    // =========================================================

    @Test
    void unConsommableSoigneUneFoisSansToucherAuPlafond() {
        Objet potion = creerObjet("potion_de_soin", CategorieObjet.OBJET, creerEffet(TypeEffet.ENDURANCE, 4));

        objetService.appliquerBonusRecuperation(personnage, potion);

        assertThat(personnage.getEnduranceMax()).isEqualTo(20);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
    }

    @Test
    void unConsommableNeSoigneJamaisAuDessusDuPlafond() {
        personnage.setEnduranceActuelle(19);
        Objet laumspur = creerObjet("laumspur", CategorieObjet.OBJET, creerEffet(TypeEffet.ENDURANCE, 3));

        objetService.appliquerBonusRecuperation(personnage, laumspur);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
    }

    @Test
    void perdreUnConsommableNeRetireRienCarDejaDepense() {
        personnage.setEnduranceMax(24);
        personnage.setEnduranceActuelle(24);
        Objet potion = creerObjet("potion_de_soin", CategorieObjet.OBJET, creerEffet(TypeEffet.ENDURANCE, 4));

        objetService.retirerBonusPerte(personnage, potion);

        assertThat(personnage.getEnduranceMax()).isEqualTo(24);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(24);
        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // HABILETE : toujours temporaire (habiliteTemp), jamais permanent
    // =========================================================

    @Test
    void unEffetHabiliteAugmenteHabiliteTempPasHabilite() {
        Objet essence = creerObjet("essence_alether", CategorieObjet.OBJET, creerEffet(TypeEffet.HABILETE, 2));

        objetService.appliquerBonusRecuperation(personnage, essence);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(2);
        assertThat(personnage.getHabilite()).isEqualTo(15); // valeur de base inchangee
    }

    @Test
    void retirerBonusPerteNeTouchePasHabiliteTemp() {
        personnage.setHabiliteTemp(2);
        Objet essence = creerObjet("essence_alether", CategorieObjet.OBJET, creerEffet(TypeEffet.HABILETE, 2));

        objetService.retirerBonusPerte(personnage, essence);

        // habiliteTemp se reinitialise seul au changement de chapitre, pas ici.
        assertThat(personnage.getHabiliteTemp()).isEqualTo(2);
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void unObjetSansEffetNeChangeRien() {
        Objet carte = creerObjet("carte", CategorieObjet.OBJETS_SPECIAUX);

        objetService.appliquerBonusRecuperation(personnage, carte);

        assertThat(personnage.getEnduranceMax()).isEqualTo(20);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(18);
        assertThat(personnage.getHabilite()).isEqualTo(15);
        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);
    }
}