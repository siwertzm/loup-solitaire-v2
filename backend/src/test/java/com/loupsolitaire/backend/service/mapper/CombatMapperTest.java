package com.loupsolitaire.backend.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.CombatEnnemiResponse;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.service.CombatService;
import com.loupsolitaire.backend.service.record.ResultatTour;

@ExtendWith(MockitoExtension.class)
class CombatMapperTest {

    private static final int CHAPITRE_ID = 17;

    @Mock
    private ChapitreRepository chapitreRepository;
    @Mock
    private CombatService combatService;

    @InjectMocks
    private CombatMapper combatMapper;

    private Chapitre chapitre;

    @BeforeEach
    void setUp() {
        chapitre = new Chapitre();
        chapitre.setId(CHAPITRE_ID);
    }

    private static Discipline discipline(IdDiscipline id) {
        Discipline discipline = new Discipline();
        discipline.setId(id);
        return discipline;
    }

    private static CombatEnnemi ennemi(String id, int enduranceActuelle) {
        Ennemi ennemi = new Ennemi();
        ennemi.setId(id);
        ennemi.setNom("Nom " + id);
        ennemi.setHabilite(15);
        ennemi.setEndurance(20);

        CombatEnnemi combatEnnemi = new CombatEnnemi();
        combatEnnemi.setEnnemi(ennemi);
        combatEnnemi.setEnduranceActuelle(enduranceActuelle);
        return combatEnnemi;
    }

    private static Combat combat(StatutCombat statut, List<CombatEnnemi> ennemis, int ennemiActifIndex) {
        Combat combat = new Combat();
        combat.setId(UUID.randomUUID());
        combat.setChapitreId(CHAPITRE_ID);
        combat.setStatut(statut);
        combat.setEnnemis(ennemis);
        combat.setEnnemiActifIndex(ennemiActifIndex);
        combat.setAssautsLivres(3);
        combat.setEndurancePerdue(true);
        combat.setBonusHabiliteEnAttente(2);
        return combat;
    }

    @Test
    void recopieLEtatDuCombat() {
        Combat combat = combat(StatutCombat.EN_COURS, List.of(ennemi("kraan", 10)), 0);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.of(chapitre));
        when(combatService.peutFuir(chapitre, combat)).thenReturn(false);

        CombatResponse reponse = combatMapper.versReponse(combat);

        assertThat(reponse.id()).isEqualTo(combat.getId());
        assertThat(reponse.chapitreId()).isEqualTo(CHAPITRE_ID);
        assertThat(reponse.assautsLivres()).isEqualTo(3);
        assertThat(reponse.endurancePerdue()).isTrue();
        assertThat(reponse.bonusHabiliteEnAttente()).isEqualTo(2);
        assertThat(reponse.statut()).isEqualTo("EN_COURS");
        assertThat(reponse.fuitePossible()).isFalse();
        // Aucun tour vient d'etre joue (GET/POST /combat).
        assertThat(reponse.dernierTour()).isNull();
    }

    @Test
    void indiqueQueLaFuiteEstPossibleSiCombatServiceLeDit() {
        Combat combat = combat(StatutCombat.EN_COURS, List.of(ennemi("kraan", 10)), 0);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.of(chapitre));
        when(combatService.peutFuir(chapitre, combat)).thenReturn(true);

        assertThat(combatMapper.versReponse(combat).fuitePossible()).isTrue();
    }

    @Test
    void unCombatTermineNePermetPlusDeFuirSansRelireLeChapitre() {
        Combat combat = combat(StatutCombat.VICTOIRE, List.of(ennemi("kraan", 0)), 1);

        assertThat(combatMapper.versReponse(combat).fuitePossible()).isFalse();

        verifyNoInteractions(chapitreRepository, combatService);
    }

    @Test
    void echoueSiLeChapitreDuCombatNExistePlus() {
        Combat combat = combat(StatutCombat.EN_COURS, List.of(ennemi("kraan", 10)), 0);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> combatMapper.versReponse(combat))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    @Test
    void marqueLEnnemiActifEtLesEnnemisVaincus() {
        CombatEnnemi vaincu = ennemi("giak_1", 0);
        CombatEnnemi actif = ennemi("giak_2", 8);
        CombatEnnemi suivant = ennemi("giak_3", 20);
        Combat combat = combat(StatutCombat.EN_COURS, List.of(vaincu, actif, suivant), 1);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.of(chapitre));

        List<CombatEnnemiResponse> ennemis = combatMapper.versReponse(combat).ennemis();

        assertThat(ennemis).extracting(CombatEnnemiResponse::id).containsExactly("giak_1", "giak_2", "giak_3");
        assertThat(ennemis).extracting(CombatEnnemiResponse::vaincu).containsExactly(true, false, false);
        assertThat(ennemis).extracting(CombatEnnemiResponse::actif).containsExactly(false, true, false);

        CombatEnnemiResponse giak2 = ennemis.get(1);
        assertThat(giak2.nom()).isEqualTo("Nom giak_2");
        assertThat(giak2.habilite()).isEqualTo(15);
        assertThat(giak2.enduranceMax()).isEqualTo(20);
        assertThat(giak2.enduranceActuelle()).isEqualTo(8);
    }

    @Test
    void listeLesResistancesEtLesDisciplinesTrieesDeLEnnemi() {
        CombatEnnemi vordak = ennemi("vordak", 18);
        vordak.getEnnemi().setResistances(List.of(discipline(IdDiscipline.PUISSANCE_PSYCHIQUE)));
        vordak.getEnnemi().setDisciplines(Set.of(
                discipline(IdDiscipline.PUISSANCE_PSYCHIQUE), discipline(IdDiscipline.CAMOUFLAGE)));
        Combat combat = combat(StatutCombat.EN_COURS, List.of(vordak), 0);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.of(chapitre));

        CombatEnnemiResponse reponse = combatMapper.versReponse(combat).ennemis().getFirst();

        assertThat(reponse.resistances()).containsExactly("PUISSANCE_PSYCHIQUE");
        // Set sans ordre en entree : la reponse est triee pour rester stable.
        assertThat(reponse.disciplines()).containsExactly("CAMOUFLAGE", "PUISSANCE_PSYCHIQUE");
    }

    @Test
    void recopieLeDetailDuTourJoue() {
        Combat combat = combat(StatutCombat.EN_COURS, List.of(ennemi("kraan", 10)), 0);
        when(chapitreRepository.findById(CHAPITRE_ID)).thenReturn(Optional.of(chapitre));
        ResultatTour tour = new ResultatTour("ATTAQUE", 2, 7, 5, -2, 3, 4, 4, null, null, null);

        CombatResponse reponse = combatMapper.versReponse(combat, tour);

        assertThat(reponse.dernierTour()).isNotNull();
        assertThat(reponse.dernierTour().action()).isEqualTo("ATTAQUE");
        assertThat(reponse.dernierTour().rapportAttaque()).isEqualTo(2);
        assertThat(reponse.dernierTour().tirageAttaque()).isEqualTo(7);
        assertThat(reponse.dernierTour().degatsInfliges()).isEqualTo(5);
        assertThat(reponse.dernierTour().rapportRiposte()).isEqualTo(-2);
        assertThat(reponse.dernierTour().tirageRiposte()).isEqualTo(3);
        assertThat(reponse.dernierTour().degatsSubisBruts()).isEqualTo(4);
        assertThat(reponse.dernierTour().degatsSubis()).isEqualTo(4);
        // Champs propres a DEFENSE : restent null (et non 0).
        assertThat(reponse.dernierTour().tirageDefense()).isNull();
        assertThat(reponse.dernierTour().reductionPourcent()).isNull();
        assertThat(reponse.dernierTour().bonusHabiliteObtenu()).isNull();
    }
}