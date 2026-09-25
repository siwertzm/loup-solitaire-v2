package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.repository.CombatRepository;

@ExtendWith(MockitoExtension.class)
class ConditionServiceTest {

    @Mock
    private InventaireService inventaireService;
    @Mock
    private CombatRepository combatRepository;

    @InjectMocks
    private ConditionService conditionService;

    private Personnage personnage;
    private Chapitre chapitre;

    @BeforeEach
    void setUp() {
        chapitre = new Chapitre();
        chapitre.setId(17);

        personnage = new Personnage();
        personnage.setChapitreActuel(chapitre);
    }

    private Cond creerCond(TypeCondition type, String targetId, String valeur) {
        Cond cond = new Cond();
        cond.setType(type);
        cond.setTargetId(targetId);
        cond.setValeur(valeur);
        return cond;
    }

    private InventaireItem creerLigne(String objetId, int quantite) {
        Objet objet = new Objet();
        objet.setId(objetId);
        InventaireItem item = new InventaireItem();
        item.setObjet(objet);
        item.setQuantite(quantite);
        return item;
    }

    private Combat creerCombat(StatutCombat statut, int assautsLivres, boolean endurancePerdue) {
        Combat combat = new Combat();
        combat.setStatut(statut);
        combat.setAssautsLivres(assautsLivres);
        combat.setEndurancePerdue(endurancePerdue);
        return combat;
    }

    private void stuberCombat(Optional<Combat> combat) {
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 17))
                .thenReturn(combat);
    }

    // =========================================================
    // DISCIPLINE
    // =========================================================

    @Test
    void disponibleSiLaDisciplineEstPossedee() {
        Discipline chasse = new Discipline();
        chasse.setId(IdDiscipline.CHASSE);
        personnage.setDisciplines(List.of(chasse));

        Cond cond = creerCond(TypeCondition.DISCIPLINE, "chasse", "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiLaDisciplineNestPasPossedee() {
        personnage.setDisciplines(List.of());
        Cond cond = creerCond(TypeCondition.DISCIPLINE, "sixieme_sens", "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    // =========================================================
    // OBJET / ARME / BOURSE (meme logique : quantite possedee)
    // =========================================================

    @Test
    void disponibleSiLObjetEstPossedeEnQuantiteSuffisante() {
        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(creerLigne("pierre_de_vordak", 1)));

        Cond cond = creerCond(TypeCondition.OBJET, "pierre_de_vordak", "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiLObjetNestPasPossede() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        Cond cond = creerCond(TypeCondition.OBJET, "pierre_de_vordak", "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void disponibleSiAssezDorPourLaBourse() {
        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(creerLigne("or", 15)));

        Cond cond = creerCond(TypeCondition.BOURSE, "or", "10");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiPasAssezDorPourLaBourse() {
        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(creerLigne("or", 5)));

        Cond cond = creerCond(TypeCondition.BOURSE, "or", "10");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void disponibleSiLArmeEstPossedee() {
        when(inventaireService.listerInventaire(personnage))
                .thenReturn(List.of(creerLigne("epee", 1)));

        Cond cond = creerCond(TypeCondition.ARME, "epee", "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    // =========================================================
    // ENDURANCE
    // =========================================================

    @Test
    void disponibleSiEnduranceSuffisante() {
        personnage.setEnduranceActuelle(15);
        Cond cond = creerCond(TypeCondition.ENDURANCE, null, "10");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiEnduranceInsuffisante() {
        personnage.setEnduranceActuelle(5);
        Cond cond = creerCond(TypeCondition.ENDURANCE, null, "10");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    // Avant : une valeur mal ecrite valait 0 en silence (ici "endurance >=
    // 0", donc toujours vrai). Desormais l'erreur est visible.
    @Test
    void uneValeurMalEcriteLeveUneErreurAuLieuDeValoirZero() {
        personnage.setEnduranceActuelle(5);
        Cond cond = creerCond(TypeCondition.ENDURANCE, null, "dix");

        assertThatThrownBy(() -> conditionService.estDisponible(cond, personnage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dix");
    }

    // =========================================================
    // HASARD (evalue via le tirage stocke)
    // =========================================================

    @Test
    void disponibleSiLeTirageEstDansLaPlage() {
        personnage.setDernierTirageHasard(3);
        Cond cond = creerCond(TypeCondition.HASARD, null, "[0, 4]");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiLeTirageEstHorsDeLaPlage() {
        personnage.setDernierTirageHasard(7);
        Cond cond = creerCond(TypeCondition.HASARD, null, "[0, 4]");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void disponibleAuxBornesDeLaPlage() {
        Cond cond = creerCond(TypeCondition.HASARD, null, "[5, 9]");

        personnage.setDernierTirageHasard(5);
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();

        personnage.setDernierTirageHasard(9);
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void indisponibleSiAucunTirageNaEncoreEteFait() {
        personnage.setDernierTirageHasard(null);
        Cond cond = creerCond(TypeCondition.HASARD, null, "[0, 9]");

        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    // =========================================================
    // Conditions de combat : dependent du dernier Combat resolu sur le
    // chapitre ACTUEL du personnage (voir CombatRepository).
    // =========================================================

    @Test
    void conditionsDeCombatSontIndisponiblesTantQuAucunCombatNExiste() {
        stuberCombat(Optional.empty());

        assertThat(conditionService.estDisponible(creerCond(TypeCondition.VICTOIRE, null, null), personnage))
                .isFalse();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.FUITE, null, null), personnage))
                .isFalse();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ASSAUT_MAX, null, "4"), personnage))
                .isFalse();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ASSAUT_ECHEC, null, "4"), personnage))
                .isFalse();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ENDURANCE_PERDUE, null, "1"), personnage))
                .isFalse();
    }

    @Test
    void victoireDisponibleUniquementSiLeCombatEstGagne() {
        Cond cond = creerCond(TypeCondition.VICTOIRE, null, null);

        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 3, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void victoireIndisponibleSiLeCombatEstEncoreEnCours() {
        Cond cond = creerCond(TypeCondition.VICTOIRE, null, null);

        stuberCombat(Optional.of(creerCombat(StatutCombat.EN_COURS, 1, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void fuiteDisponibleUniquementSiLeCombatSestTermineParUneFuite() {
        Cond cond = creerCond(TypeCondition.FUITE, null, null);

        stuberCombat(Optional.of(creerCombat(StatutCombat.FUITE, 3, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void assautMaxDisponibleSiVictoireEnPeuDassauts() {
        Cond cond = creerCond(TypeCondition.ASSAUT_MAX, null, "4");

        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 4, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void assautMaxIndisponibleSiVictoireApresTropDassauts() {
        Cond cond = creerCond(TypeCondition.ASSAUT_MAX, null, "4");

        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 5, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void assautMaxIndisponibleSiCeNestPasUneVictoire() {
        Cond cond = creerCond(TypeCondition.ASSAUT_MAX, null, "4");

        stuberCombat(Optional.of(creerCombat(StatutCombat.FUITE, 1, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void assautEchecDisponibleSiInterrompuApresLeSeuil() {
        Cond cond = creerCond(TypeCondition.ASSAUT_ECHEC, null, "4");

        stuberCombat(Optional.of(creerCombat(StatutCombat.INTERROMPU, 4, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void assautEchecIndisponibleSiPasEncoreInterrompu() {
        Cond cond = creerCond(TypeCondition.ASSAUT_ECHEC, null, "4");

        stuberCombat(Optional.of(creerCombat(StatutCombat.EN_COURS, 4, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    @Test
    void enduranceProdueDisponibleSiVictoireAvecPerteDEndurance() {
        Cond cond = creerCond(TypeCondition.ENDURANCE_PERDUE, null, "1");

        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 3, true)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void enduranceProdueDisponibleSiVictoireSansPerteDEnduranceEtValeurZero() {
        // valeur "0" : la condition demande explicitement de n'avoir RIEN perdu
        // (voir chapitre 227 : "si vous tuez sans perdre aucun point
        // d'ENDURANCE"). (valeur == 1) == combat.isEndurancePerdue()
        // -> (0==1)==false -> false==false -> true.
        Cond cond = creerCond(TypeCondition.ENDURANCE_PERDUE, null, "0");

        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 3, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void enduranceProdueIndisponibleSiLaPerteNeCorrespondPasALaValeurDemandee() {
        Cond cond = creerCond(TypeCondition.ENDURANCE_PERDUE, null, "1");

        // valeur "1" (perte attendue) mais combat gagne SANS perte -> false.
        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 3, false)));
        assertThat(conditionService.estDisponible(cond, personnage)).isFalse();
    }

    // =========================================================
    // PERMANENT : marqueur, toujours disponible (aucune dependance externe)
    // =========================================================

    @Test
    void permanentEstToujoursDisponible() {
        Cond cond = creerCond(TypeCondition.PERMANENT, null, null);

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    // =========================================================
    // estLienDisponible : un Lien n'est empruntable que si TOUTES ses
    // conditions sont remplies.
    // =========================================================

    @Test
    void lienDisponibleSiToutesSesConditionsSontRemplies() {
        personnage.setEnduranceActuelle(15);
        Discipline chasse = new Discipline();
        chasse.setId(IdDiscipline.CHASSE);
        personnage.setDisciplines(List.of(chasse));

        Lien lien = new Lien();
        lien.setConditions(List.of(
                creerCond(TypeCondition.ENDURANCE, null, "10"),
                creerCond(TypeCondition.DISCIPLINE, "chasse", "1")));

        assertThat(conditionService.estLienDisponible(lien, personnage)).isTrue();
    }

    @Test
    void lienIndisponibleSiUneSeuleConditionEchoue() {
        personnage.setEnduranceActuelle(15);
        personnage.setDisciplines(List.of());

        Lien lien = new Lien();
        lien.setConditions(List.of(
                creerCond(TypeCondition.ENDURANCE, null, "10"),
                creerCond(TypeCondition.DISCIPLINE, "chasse", "1")));

        assertThat(conditionService.estLienDisponible(lien, personnage)).isFalse();
    }

    @Test
    void lienSansAucuneConditionEstToujoursDisponible() {
        Lien lien = new Lien();
        lien.setConditions(List.of());

        assertThat(conditionService.estLienDisponible(lien, personnage)).isTrue();
    }

    // =========================================================
    // ContexteConditions : inventaire et combat lus AU PLUS UNE FOIS pour
    // toutes les conditions d'un meme chapitre (GET /chapitre).
    // =========================================================

    @Test
    void unContexteNeLitLInventaireQuUneSeuleFoisPourPlusieursConditions() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(
                creerLigne("or", 12), creerLigne("corde", 1)));
        ContexteConditions contexte = conditionService.nouveauContexte(personnage);

        Lien lienOr = new Lien();
        lienOr.setConditions(List.of(creerCond(TypeCondition.BOURSE, "or", "10")));
        Lien lienCorde = new Lien();
        lienCorde.setConditions(List.of(creerCond(TypeCondition.OBJET, "corde", "1")));
        Lien lienLanterne = new Lien();
        lienLanterne.setConditions(List.of(creerCond(TypeCondition.OBJET, "lanterne", "1")));

        assertThat(conditionService.estLienDisponible(lienOr, personnage, contexte)).isTrue();
        assertThat(conditionService.estLienDisponible(lienCorde, personnage, contexte)).isTrue();
        assertThat(conditionService.estLienDisponible(lienLanterne, personnage, contexte)).isFalse();

        verify(inventaireService, times(1)).listerInventaire(personnage);
    }

    @Test
    void unContexteAdditionneLesQuantitesDUnMemeObjet() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(
                creerLigne("or", 6), creerLigne("or", 5)));
        ContexteConditions contexte = conditionService.nouveauContexte(personnage);

        assertThat(conditionService.estDisponible(creerCond(TypeCondition.BOURSE, "or", "11"), personnage, contexte))
                .isTrue();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.BOURSE, "or", "12"), personnage, contexte))
                .isFalse();
    }

    @Test
    void unContexteNeLitLeCombatQuUneSeuleFoisPourPlusieursConditions() {
        stuberCombat(Optional.of(creerCombat(StatutCombat.VICTOIRE, 3, false)));
        ContexteConditions contexte = conditionService.nouveauContexte(personnage);

        assertThat(conditionService.estDisponible(creerCond(TypeCondition.VICTOIRE, null, null), personnage, contexte))
                .isTrue();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.FUITE, null, null), personnage, contexte))
                .isFalse();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ASSAUT_MAX, null, "4"), personnage, contexte))
                .isTrue();

        verify(combatRepository, times(1)).findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 17);
    }

    @Test
    void unContexteNeLitRienSiAucuneConditionNEnABesoin() {
        personnage.setEnduranceActuelle(15);
        ContexteConditions contexte = conditionService.nouveauContexte(personnage);

        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ENDURANCE, null, "10"), personnage, contexte))
                .isTrue();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.PERMANENT, null, null), personnage, contexte))
                .isTrue();

        verifyNoInteractions(inventaireService, combatRepository);
    }
}