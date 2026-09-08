package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.TypeCondition;

@ExtendWith(MockitoExtension.class)
class ConditionServiceTest {

    @Mock
    private InventaireService inventaireService;

    @InjectMocks
    private ConditionService conditionService;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
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

    // =========================================================
    // Conditions dynamiques : toujours disponibles (non pre-evaluables)
    // =========================================================

    @Test
    void hasardEstToujoursDisponible() {
        Cond cond = creerCond(TypeCondition.HASARD, null, "[0, 4]");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void fuiteEstToujoursDisponible() {
        Cond cond = creerCond(TypeCondition.FUITE, null, "3");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void assautMaxEtAssautEchecSontToujoursDisponibles() {
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ASSAUT_MAX, null, "4"), personnage))
                .isTrue();
        assertThat(conditionService.estDisponible(creerCond(TypeCondition.ASSAUT_ECHEC, null, "4"), personnage))
                .isTrue();
    }

    @Test
    void enduranceProdueEstToujoursDisponible() {
        Cond cond = creerCond(TypeCondition.ENDURANCE_PERDUE, null, "1");

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }

    @Test
    void permanentEstToujoursDisponible() {
        Cond cond = creerCond(TypeCondition.PERMANENT, null, null);

        assertThat(conditionService.estDisponible(cond, personnage)).isTrue();
    }
}