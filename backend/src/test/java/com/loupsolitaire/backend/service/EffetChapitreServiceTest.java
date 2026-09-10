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
    @Mock
    private ConditionService conditionService;

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

    // =========================================================
    // HABILETE
    // =========================================================

    private com.loupsolitaire.backend.model.Effet creerEffetHabilite(int valeur,
            com.loupsolitaire.backend.model.enums.TypeCondition typeCondition, String targetId) {
        com.loupsolitaire.backend.model.Effet effet = new com.loupsolitaire.backend.model.Effet();
        effet.setType(com.loupsolitaire.backend.model.enums.TypeEffet.HABILETE);
        effet.setValeur(valeur);
        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond = new com.loupsolitaire.backend.model.Cond();
            cond.setType(typeCondition);
            cond.setTargetId(targetId);
            effet.setConditions(List.of(cond));
        } else {
            effet.setConditions(List.of());
        }
        return effet;
    }

    @Test
    void appliqueDirectementAHabiliteTempSiAucuneCondition() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(4, null, null);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(4);
        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliqueLeMalusSiLaDisciplineRequiseEstAbsente() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                -2, com.loupsolitaire.backend.model.enums.TypeCondition.DISCIPLINE, "bouclier_psychique");
        personnage.setDisciplines(List.of());

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(-2);
    }

    @Test
    void nAppliqueRienSiLaDisciplineRequiseEstPossedee() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                -2, com.loupsolitaire.backend.model.enums.TypeCondition.DISCIPLINE, "bouclier_psychique");
        Discipline bouclier = new Discipline();
        bouclier.setId(IdDiscipline.BOUCLIER_PSYCHIQUE);
        personnage.setDisciplines(List.of(bouclier));

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void appliqueLeMalusSiLObjetRequisEstAbsent() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                -3, com.loupsolitaire.backend.model.enums.TypeCondition.OBJET, "torche");
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(-3);
    }

    @Test
    void nAppliqueRienSiLObjetRequisEstPossede() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                -3, com.loupsolitaire.backend.model.enums.TypeCondition.OBJET, "torche");
        Objet torche = new Objet();
        torche.setId("torche");
        InventaireItem ligne = new InventaireItem();
        ligne.setObjet(torche);
        ligne.setQuantite(1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(ligne));

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);
    }

    @Test
    void appliquePermanentAHabiliteBaseEtRecalculeLHabiliteEffective() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                -1, com.loupsolitaire.backend.model.enums.TypeCondition.PERMANENT, null);
        personnage.setHabiliteBase(15);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteBase()).isEqualTo(14);
        assertThat(personnage.getHabiliteTemp()).isEqualTo(0); // pas touchee
        verify(inventaireService).recalculerHabiliteArmes(personnage);
    }

    @Test
    void ignoreLesConditionsDeCombatNonConstruites() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetHabilite(
                2, com.loupsolitaire.backend.model.enums.TypeCondition.ASSAUT_MAX, null);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);
        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // ENDURANCE
    // =========================================================

    private com.loupsolitaire.backend.model.Effet creerEffetEndurance(int valeur,
            com.loupsolitaire.backend.model.enums.TypeCondition typeCondition, String valeurCondition) {
        com.loupsolitaire.backend.model.Effet effet = new com.loupsolitaire.backend.model.Effet();
        effet.setType(com.loupsolitaire.backend.model.enums.TypeEffet.ENDURANCE);
        effet.setValeur(valeur);
        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond = new com.loupsolitaire.backend.model.Cond();
            cond.setType(typeCondition);
            cond.setValeur(valeurCondition);
            effet.setConditions(List.of(cond));
        } else {
            effet.setConditions(List.of());
        }
        return effet;
    }

    @Test
    void appliqueDirectementSiAucuneCondition() {
        personnage.setEnduranceMax(20);
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(-2, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(13); // 15 - 2
        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliqueSiLeTirageHasardEstDansLaPlage() {
        personnage.setEnduranceMax(20);
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(
                -2, com.loupsolitaire.backend.model.enums.TypeCondition.HASARD, "[0, 4]");
        when(conditionService.estDisponible(effet.getConditions().get(0), personnage)).thenReturn(true);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(13);
    }

    @Test
    void nAppliqueRienSiLeTirageHasardEstHorsPlage() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(
                -2, com.loupsolitaire.backend.model.enums.TypeCondition.HASARD, "[0, 4]");
        when(conditionService.estDisponible(effet.getConditions().get(0), personnage)).thenReturn(false);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15); // inchangee
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void neDepasseJamaisLeplafondEnduranceMax() {
        personnage.setEnduranceMax(20);
        personnage.setEnduranceActuelle(19);
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(10, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
    }

    @Test
    void neDescendJamaisSousZeroPourUnEffetEndurance() {
        personnage.setEnduranceActuelle(1);
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(-10, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(0);
    }

    @Test
    void ignoreUneConditionDeCombatNonConstruitePourEndurance() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetEndurance(
                -2, com.loupsolitaire.backend.model.enums.TypeCondition.ENDURANCE_PERDUE, "1");

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15); // inchangee
        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // VOL
    // =========================================================

    private com.loupsolitaire.backend.model.Effet creerEffetVol(int valeur,
            com.loupsolitaire.backend.model.enums.TypeCondition typeCondition, String valeurCondition) {
        com.loupsolitaire.backend.model.Effet effet = new com.loupsolitaire.backend.model.Effet();
        effet.setType(com.loupsolitaire.backend.model.enums.TypeEffet.VOL);
        effet.setValeur(valeur);
        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond = new com.loupsolitaire.backend.model.Cond();
            cond.setType(typeCondition);
            cond.setValeur(valeurCondition);
            effet.setConditions(List.of(cond));
        } else {
            effet.setConditions(List.of());
        }
        return effet;
    }

    private InventaireItem creerLigne(String objetId, CategorieObjet categorie, int quantite) {
        Objet objet = new Objet();
        objet.setId(objetId);
        objet.setCategorie(categorie);
        InventaireItem item = new InventaireItem();
        item.setObjet(objet);
        item.setQuantite(quantite);
        return item;
    }

    @Test
    void vol10RetireToutSacEtArmes() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(10, null, null);
        InventaireItem repas = creerLigne("repas", CategorieObjet.REPAS, 2);
        InventaireItem hache = creerLigne("hache", CategorieObjet.ARME, 1);
        InventaireItem or = creerLigne("or", CategorieObjet.BOURSE, 20); // ne doit pas etre touche
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(repas, hache, or));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService).retirerObjet(personnage, repas.getObjet(), 2);
        verify(inventaireService).retirerObjet(personnage, hache.getObjet(), 1);
        verify(inventaireService, never()).retirerObjet(personnage, or.getObjet(), 20);
    }

    @Test
    void vol8RetireSeulementLeSacPasLesArmes() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(8, null, null);
        InventaireItem repas = creerLigne("repas", CategorieObjet.REPAS, 1);
        InventaireItem hache = creerLigne("hache", CategorieObjet.ARME, 1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(repas, hache));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService).retirerObjet(personnage, repas.getObjet(), 1);
        verify(inventaireService, never()).retirerObjet(personnage, hache.getObjet(), 1);
    }

    @Test
    void vol2AvecConditionArmeRetireToutesLesArmes() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(
                2, com.loupsolitaire.backend.model.enums.TypeCondition.ARME, "1");
        InventaireItem hache = creerLigne("hache", CategorieObjet.ARME, 1);
        InventaireItem glaive = creerLigne("glaive", CategorieObjet.ARME, 1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(hache, glaive));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService).retirerObjet(personnage, hache.getObjet(), 1);
        verify(inventaireService).retirerObjet(personnage, glaive.getObjet(), 1);
    }

    @Test
    void vol1AvecConditionArmeMarqueUneAttenteDePorteeArme() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(
                1, com.loupsolitaire.backend.model.enums.TypeCondition.ARME, "1");
        InventaireItem hache = creerLigne("hache", CategorieObjet.ARME, 1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(hache));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isEqualTo(com.loupsolitaire.backend.model.enums.PorteeVol.ARME);
        verify(personnageRepository).save(personnage);
        verify(inventaireService, never()).retirerObjet(any(), any(), anyInt());
    }

    @Test
    void vol1SansConditionMarqueUneAttenteDePorteeTout() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(1, null, null);
        InventaireItem repas = creerLigne("repas", CategorieObjet.REPAS, 1);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(repas));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isEqualTo(com.loupsolitaire.backend.model.enums.PorteeVol.TOUT);
    }

    @Test
    void vol1AvecConditionArmeNeBloquePasSiAucuneArmePossedee() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(
                1, com.loupsolitaire.backend.model.enums.TypeCondition.ARME, "1");
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isNull();
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void vol1SansConditionNeBloquePasSiInventaireCompletementVide() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(1, null, null);
        InventaireItem or = creerLigne("or", CategorieObjet.BOURSE, 20); // pas eligible (bourse)
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(or));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isNull();
        verify(personnageRepository, never()).save(any());
    }

    @Test
    void volGateParHasardNeSeProduitPasSiTirageHorsPlage() {
        com.loupsolitaire.backend.model.Effet effet = creerEffetVol(
                8, com.loupsolitaire.backend.model.enums.TypeCondition.HASARD, "[0, 6]");
        when(conditionService.estDisponible(effet.getConditions().get(0), personnage)).thenReturn(false);

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService, never()).listerInventaire(any());
        verify(inventaireService, never()).retirerObjet(any(), any(), anyInt());
    }

    @Test
    void resoudreVolEnAttentePorteeArmeAccepteUneArme() {
        personnage.setVolEnAttente(com.loupsolitaire.backend.model.enums.PorteeVol.ARME);
        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        effetChapitreService.resoudreVolEnAttente(personnage, hache);

        verify(inventaireService).retirerObjet(personnage, hache, 1);
        assertThat(personnage.getVolEnAttente()).isNull();
    }

    @Test
    void resoudreVolEnAttentePorteeArmeRefuseUnObjetNonArme() {
        personnage.setVolEnAttente(com.loupsolitaire.backend.model.enums.PorteeVol.ARME);
        Objet repas = new Objet();
        repas.setId("repas");
        repas.setCategorie(CategorieObjet.REPAS);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> effetChapitreService.resoudreVolEnAttente(personnage, repas))
                .isInstanceOf(IllegalArgumentException.class);
        verify(inventaireService, never()).retirerObjet(any(), any(), anyInt());
    }

    @Test
    void resoudreVolEnAttenteEchoueSiAucunVolEnAttente() {
        personnage.setVolEnAttente(null);
        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> effetChapitreService.resoudreVolEnAttente(personnage, hache))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resoudreVolEnAttenteEchoueSiLePersonnageEstMort() {
        personnage.setMort(true);
        personnage.setVolEnAttente(com.loupsolitaire.backend.model.enums.PorteeVol.ARME);
        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> effetChapitreService.resoudreVolEnAttente(personnage, hache))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(inventaireService, never()).retirerObjet(any(), any(), anyInt());
        verify(personnageRepository, never()).save(any());
    }
}