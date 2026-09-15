package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.PorteeVol;
import com.loupsolitaire.backend.model.enums.StatutRepas;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.model.enums.TypeEffet;
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

    // =========================================================
    // REPAS
    // =========================================================

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

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(ligneRepas));

        effetChapitreService.appliquerEffetRepas(personnage);

        verify(inventaireService)
                .retirerObjet(personnage, ligneRepas.getObjet(), 1);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15);
        assertThat(personnage.getDernierStatutRepas())
                .isEqualTo(StatutRepas.REPAS_CONSOMME);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliqueMoins3EnduranceSiAucunRepasDisponible() {
        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of());

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(12);
        assertThat(personnage.getDernierStatutRepas())
                .isEqualTo(StatutRepas.MALUS_ENDURANCE);

        verify(inventaireService, never())
                .retirerObjet(any(), any(), eq(1));

        verify(personnageRepository).save(personnage);
    }

    @Test
    void neDescendJamaisEnDessousDeZero() {
        personnage.setEnduranceActuelle(1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of());

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(0);
        assertThat(personnage.isMort()).isTrue();
        assertThat(personnage.getDernierStatutRepas())
                .isEqualTo(StatutRepas.MALUS_ENDURANCE);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void laDisciplineChasseDispenseCompletementDeLaRegle() {
        Discipline chasse = new Discipline();
        chasse.setId(IdDiscipline.CHASSE);

        personnage.setDisciplines(List.of(chasse));

        effetChapitreService.appliquerEffetRepas(personnage);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15);
        assertThat(personnage.getDernierStatutRepas())
                .isEqualTo(StatutRepas.CHASSE);

        verify(inventaireService, never()).listerInventaire(any());
        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());

        verify(personnageRepository).save(personnage);
    }

    @Test
    void neConsommeQuUnSeulRepasMemeSiPlusieursPossedes() {
        InventaireItem ligneRepas = creerLigneRepas(5);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(ligneRepas));

        effetChapitreService.appliquerEffetRepas(personnage);

        verify(inventaireService)
                .retirerObjet(personnage, ligneRepas.getObjet(), 1);

        assertThat(personnage.getDernierStatutRepas())
                .isEqualTo(StatutRepas.REPAS_CONSOMME);

        verify(personnageRepository).save(personnage);
    }

    // =========================================================
    // HABILITE
    // =========================================================

    private Effet creerEffetHabilite(
            int valeur,
            TypeCondition typeCondition,
            String targetId) {

        Effet effet = new Effet();
        effet.setType(TypeEffet.HABILITE);
        effet.setValeur(valeur);

        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond =
                    new com.loupsolitaire.backend.model.Cond();

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
        Effet effet = creerEffetHabilite(4, null, null);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(4);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliqueLeMalusSiLaDisciplineRequiseEstAbsente() {
        Effet effet = creerEffetHabilite(
                -2,
                TypeCondition.DISCIPLINE,
                "bouclier_psychique");

        personnage.setDisciplines(List.of());

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(-2);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void nAppliqueRienSiLaDisciplineRequiseEstPossedee() {
        Effet effet = creerEffetHabilite(
                -2,
                TypeCondition.DISCIPLINE,
                "bouclier_psychique");

        Discipline bouclier = new Discipline();
        bouclier.setId(IdDiscipline.BOUCLIER_PSYCHIQUE);

        personnage.setDisciplines(List.of(bouclier));

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void appliqueLeMalusSiLObjetRequisEstAbsent() {
        Effet effet = creerEffetHabilite(
                -3,
                TypeCondition.OBJET,
                "torche");

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of());

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(-3);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void nAppliqueRienSiLObjetRequisEstPossede() {
        Effet effet = creerEffetHabilite(
                -3,
                TypeCondition.OBJET,
                "torche");

        Objet torche = new Objet();
        torche.setId("torche");

        InventaireItem ligne = new InventaireItem();
        ligne.setObjet(torche);
        ligne.setQuantite(1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(ligne));

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void appliquePermanentAHabiliteBaseEtRecalculeLHabiliteEffective() {
        Effet effet = creerEffetHabilite(
                -1,
                TypeCondition.PERMANENT,
                null);

        personnage.setHabiliteBase(15);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteBase()).isEqualTo(14);
        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);

        verify(personnageRepository).save(personnage);
        verify(inventaireService).recalculerHabiliteArmes(personnage);
    }

    @Test
    void ignoreLesConditionsDeCombatNonConstruites() {
        Effet effet = creerEffetHabilite(
                2,
                TypeCondition.ASSAUT_MAX,
                null);

        effetChapitreService.appliquerEffetHabilite(personnage, effet);

        assertThat(personnage.getHabiliteTemp()).isEqualTo(0);

        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // ENDURANCE
    // =========================================================

    private Effet creerEffetEndurance(
            int valeur,
            TypeCondition typeCondition,
            String valeurCondition) {

        Effet effet = new Effet();
        effet.setType(TypeEffet.ENDURANCE);
        effet.setValeur(valeur);

        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond =
                    new com.loupsolitaire.backend.model.Cond();

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

        Effet effet = creerEffetEndurance(-2, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(13);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void appliqueSiLeTirageHasardEstDansLaPlage() {
        personnage.setEnduranceMax(20);

        Effet effet = creerEffetEndurance(
                -2,
                TypeCondition.HASARD,
                "[0, 4]");

        when(conditionService.estDisponible(
                effet.getConditions().get(0),
                personnage))
                .thenReturn(true);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(13);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void nAppliqueRienSiLeTirageHasardEstHorsPlage() {
        Effet effet = creerEffetEndurance(
                -2,
                TypeCondition.HASARD,
                "[0, 4]");

        when(conditionService.estDisponible(
                effet.getConditions().get(0),
                personnage))
                .thenReturn(false);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15);

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void neDepasseJamaisLeplafondEnduranceMax() {
        personnage.setEnduranceMax(20);
        personnage.setEnduranceActuelle(19);

        Effet effet = creerEffetEndurance(10, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void neDescendJamaisSousZeroPourUnEffetEndurance() {
        personnage.setEnduranceActuelle(1);

        Effet effet = creerEffetEndurance(-10, null, null);

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(0);
        assertThat(personnage.isMort()).isTrue();

        verify(personnageRepository).save(personnage);
    }

    @Test
    void ignoreUneConditionDeCombatNonConstruitePourEndurance() {
        Effet effet = creerEffetEndurance(
                -2,
                TypeCondition.ENDURANCE_PERDUE,
                "1");

        effetChapitreService.appliquerEffetEndurance(personnage, effet);

        assertThat(personnage.getEnduranceActuelle()).isEqualTo(15);

        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // VOL
    // =========================================================

    private Effet creerEffetVol(
            int valeur,
            TypeCondition typeCondition,
            String valeurCondition) {

        Effet effet = new Effet();
        effet.setType(TypeEffet.VOL);
        effet.setValeur(valeur);

        if (typeCondition != null) {
            com.loupsolitaire.backend.model.Cond cond =
                    new com.loupsolitaire.backend.model.Cond();

            cond.setType(typeCondition);
            cond.setValeur(valeurCondition);

            effet.setConditions(List.of(cond));
        } else {
            effet.setConditions(List.of());
        }

        return effet;
    }

    private InventaireItem creerLigne(
            String objetId,
            CategorieObjet categorie,
            int quantite) {

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
        Effet effet = creerEffetVol(10, null, null);

        InventaireItem repas =
                creerLigne("repas", CategorieObjet.REPAS, 2);

        InventaireItem hache =
                creerLigne("hache", CategorieObjet.ARME, 1);

        InventaireItem or =
                creerLigne("or", CategorieObjet.BOURSE, 20);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(repas, hache, or));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService)
                .retirerObjet(personnage, repas.getObjet(), 2);

        verify(inventaireService)
                .retirerObjet(personnage, hache.getObjet(), 1);

        verify(inventaireService, never())
                .retirerObjet(personnage, or.getObjet(), 20);
    }

    @Test
    void vol8RetireSeulementLeSacPasLesArmes() {
        Effet effet = creerEffetVol(8, null, null);

        InventaireItem repas =
                creerLigne("repas", CategorieObjet.REPAS, 1);

        InventaireItem hache =
                creerLigne("hache", CategorieObjet.ARME, 1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(repas, hache));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService)
                .retirerObjet(personnage, repas.getObjet(), 1);

        verify(inventaireService, never())
                .retirerObjet(personnage, hache.getObjet(), 1);
    }

    @Test
    void vol2AvecConditionArmeRetireToutesLesArmes() {
        Effet effet = creerEffetVol(
                2,
                TypeCondition.ARME,
                "1");

        InventaireItem hache =
                creerLigne("hache", CategorieObjet.ARME, 1);

        InventaireItem glaive =
                creerLigne("glaive", CategorieObjet.ARME, 1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(hache, glaive));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService)
                .retirerObjet(personnage, hache.getObjet(), 1);

        verify(inventaireService)
                .retirerObjet(personnage, glaive.getObjet(), 1);
    }

    @Test
    void vol1AvecConditionArmeMarqueUneAttenteDePorteeArme() {
        Effet effet = creerEffetVol(
                1,
                TypeCondition.ARME,
                "1");

        InventaireItem hache =
                creerLigne("hache", CategorieObjet.ARME, 1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(hache));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente())
                .isEqualTo(PorteeVol.ARME);

        verify(personnageRepository).save(personnage);

        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());
    }

    @Test
    void vol1SansConditionMarqueUneAttenteDePorteeTout() {
        Effet effet = creerEffetVol(1, null, null);

        InventaireItem repas =
                creerLigne("repas", CategorieObjet.REPAS, 1);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(repas));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente())
                .isEqualTo(PorteeVol.TOUT);

        verify(personnageRepository).save(personnage);
    }

    @Test
    void vol1AvecConditionArmeNeBloquePasSiAucuneArmePossedee() {
        Effet effet = creerEffetVol(
                1,
                TypeCondition.ARME,
                "1");

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of());

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isNull();

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void vol1SansConditionNeBloquePasSiInventaireCompletementVide() {
        Effet effet = creerEffetVol(1, null, null);

        InventaireItem or =
                creerLigne("or", CategorieObjet.BOURSE, 20);

        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(or));

        effetChapitreService.appliquerEffetVol(personnage, effet);

        assertThat(personnage.getVolEnAttente()).isNull();

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void volGateParHasardNeSeProduitPasSiTirageHorsPlage() {
        Effet effet = creerEffetVol(
                8,
                TypeCondition.HASARD,
                "[0, 6]");

        when(conditionService.estDisponible(
                effet.getConditions().get(0),
                personnage))
                .thenReturn(false);

        effetChapitreService.appliquerEffetVol(personnage, effet);

        verify(inventaireService, never()).listerInventaire(any());

        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void resoudreVolEnAttentePorteeArmeAccepteUneArme() {
        personnage.setVolEnAttente(PorteeVol.ARME);

        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        effetChapitreService.resoudreVolEnAttente(personnage, hache);

        verify(inventaireService)
                .retirerObjet(personnage, hache, 1);

        assertThat(personnage.getVolEnAttente()).isNull();

        verify(personnageRepository).save(personnage);
    }

    @Test
    void resoudreVolEnAttentePorteeArmeRefuseUnObjetNonArme() {
        personnage.setVolEnAttente(PorteeVol.ARME);

        Objet repas = new Objet();
        repas.setId("repas");
        repas.setCategorie(CategorieObjet.REPAS);

        assertThatThrownBy(
                () -> effetChapitreService.resoudreVolEnAttente(
                        personnage,
                        repas))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void resoudreVolEnAttenteEchoueSiAucunVolEnAttente() {
        personnage.setVolEnAttente(null);

        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        assertThatThrownBy(
                () -> effetChapitreService.resoudreVolEnAttente(
                        personnage,
                        hache))
                .isInstanceOf(IllegalStateException.class);

        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());

        verify(personnageRepository, never()).save(any());
    }

    @Test
    void resoudreVolEnAttenteEchoueSiLePersonnageEstMort() {
        personnage.setMort(true);
        personnage.setVolEnAttente(PorteeVol.ARME);

        Objet hache = new Objet();
        hache.setId("hache");
        hache.setCategorie(CategorieObjet.ARME);

        assertThatThrownBy(
                () -> effetChapitreService.resoudreVolEnAttente(
                        personnage,
                        hache))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(inventaireService, never())
                .retirerObjet(any(), any(), anyInt());

        verify(personnageRepository, never()).save(any());
    }
}