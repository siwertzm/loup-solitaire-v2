package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ResultatTour;
import com.loupsolitaire.backend.service.record.TourJoue;

@ExtendWith(MockitoExtension.class)
class CombatServiceTest {

    @Mock
    private CombatRepository combatRepository;
    @Mock
    private ChapitreRepository chapitreRepository;
    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private TableDeHasardService tableDeHasardService;
    @Mock
    private TableCombatService tableCombatService;
    @Mock
    private InventaireService inventaireService;
    @Mock
    private ObjetService objetService;

    @InjectMocks
    private CombatService combatService;

    // =========================================================
    // Helpers de construction
    // =========================================================

    private Ennemi creerEnnemi(String id, int habilite, int endurance) {
        Ennemi ennemi = new Ennemi();
        ennemi.setId(id);
        ennemi.setHabilite(habilite);
        ennemi.setEndurance(endurance);
        return ennemi;
    }

    private Chapitre creerChapitreCombat(int id, List<Ennemi> ennemis, List<Lien> liens) {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(id);
        chapitre.setCombat(true);
        chapitre.setEnnemis(ennemis);
        chapitre.setLiens(liens != null ? liens : new ArrayList<>());
        return chapitre;
    }

    private Personnage creerPersonnage(int habilite, int enduranceActuelle, Chapitre chapitreActuel) {
        Personnage personnage = new Personnage();
        personnage.setId(UUID.randomUUID());
        personnage.setHabilite(habilite);
        personnage.setHabiliteTemp(0);
        personnage.setEnduranceActuelle(enduranceActuelle);
        personnage.setChapitreActuel(chapitreActuel);
        personnage.setDisciplines(new ArrayList<>());
        personnage.setMort(false);
        return personnage;
    }

    private CombatEnnemi creerCombatEnnemi(Ennemi ennemi, int ordre, int enduranceActuelle) {
        CombatEnnemi ce = new CombatEnnemi();
        ce.setEnnemi(ennemi);
        ce.setOrdre(ordre);
        ce.setEnduranceActuelle(enduranceActuelle);
        return ce;
    }

    private Combat creerCombatEnCours(Chapitre chapitre, List<CombatEnnemi> ennemis) {
        Combat combat = new Combat();
        combat.setId(UUID.randomUUID());
        combat.setChapitreId(chapitre.getId());
        combat.setEnnemiActifIndex(0);
        combat.setAssautsLivres(0);
        combat.setEndurancePerdue(false);
        combat.setBonusHabiliteEnAttente(0);
        combat.setStatut(StatutCombat.EN_COURS);
        combat.setCreeLe(Instant.now());
        combat.setEnnemis(ennemis);
        return combat;
    }

    private Lien creerLienAvecCondition(TypeCondition type, String valeur) {
        Cond cond = new Cond();
        cond.setType(type);
        cond.setValeur(valeur);

        Lien lien = new Lien();
        lien.setConditions(List.of(cond));
        return lien;
    }

    // Stub commun : le combat en cours est retrouve tel quel (pas recree).
    // save() est marque lenient : plusieurs tests d'echec (combat deja
    // termine, objet invalide, fuite refusee...) s'arretent avant de
    // l'atteindre, ce qui rendrait ce stub "inutile" en mode strict.
    private void stuberCombatExistant(Personnage personnage, Chapitre chapitre, Combat combat) {
        when(chapitreRepository.findById(chapitre.getId())).thenReturn(Optional.of(chapitre));
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, chapitre.getId()))
                .thenReturn(Optional.of(combat));
        lenient().when(combatRepository.save(any(Combat.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================
    // initierCombat
    // =========================================================

    @Test
    void initierCombatCreeUnNouveauCombatQuandAucunNExiste() {
        Ennemi kraan = creerEnnemi("kraan", 15, 8);
        Chapitre chapitre = creerChapitreCombat(17, List.of(kraan), null);
        Personnage personnage = creerPersonnage(20, 20, chapitre);

        when(chapitreRepository.findById(17)).thenReturn(Optional.of(chapitre));
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 17))
                .thenReturn(Optional.empty());
        when(combatRepository.save(any(Combat.class))).thenAnswer(inv -> inv.getArgument(0));

        Combat combat = combatService.initierCombat(personnage);

        assertThat(combat.getStatut()).isEqualTo(StatutCombat.EN_COURS);
        assertThat(combat.getChapitreId()).isEqualTo(17);
        assertThat(combat.getEnnemiActifIndex()).isZero();
        assertThat(combat.getEnnemis()).hasSize(1);
        assertThat(combat.getEnnemis().get(0).getEnduranceActuelle()).isEqualTo(8);
        assertThat(combat.getEnnemis().get(0).getEnnemi()).isEqualTo(kraan);
    }

    @Test
    void initierCombatRenvoieLeCombatExistantSansEnCreerUnNouveau() {
        Ennemi kraan = creerEnnemi("kraan", 15, 8);
        Chapitre chapitre = creerChapitreCombat(17, List.of(kraan), null);
        Personnage personnage = creerPersonnage(20, 20, chapitre);
        Combat combatExistant = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(kraan, 0, 3)));

        when(chapitreRepository.findById(17)).thenReturn(Optional.of(chapitre));
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 17))
                .thenReturn(Optional.of(combatExistant));

        Combat resultat = combatService.initierCombat(personnage);

        assertThat(resultat).isSameAs(combatExistant);
        verify(combatRepository, never()).save(any());
    }

    @Test
    void initierCombatEchoueSiLeChapitreNEstPasUnCombat() {
        Chapitre chapitre = creerChapitreCombat(5, List.of(), null);
        chapitre.setCombat(false);
        Personnage personnage = creerPersonnage(20, 20, chapitre);

        when(chapitreRepository.findById(5)).thenReturn(Optional.of(chapitre));

        assertThatThrownBy(() -> combatService.initierCombat(personnage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("n'est pas un combat");
    }

    @Test
    void initierCombatEchoueSiLePersonnageEstMort() {
        Personnage personnage = creerPersonnage(20, 0, null);
        personnage.setMort(true);

        assertThatThrownBy(() -> combatService.initierCombat(personnage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(chapitreRepository, never()).findById(any());
    }

    // =========================================================
    // combatActuel
    // =========================================================

    @Test
    void combatActuelRenvoieVideSiAucunCombatN_existe() {
        Chapitre chapitre = creerChapitreCombat(17, List.of(), null);
        Personnage personnage = creerPersonnage(20, 20, chapitre);

        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, 17))
                .thenReturn(Optional.empty());

        assertThat(combatService.combatActuel(personnage)).isEmpty();
    }

    // =========================================================
    // jouerTour - ATTAQUE
    // =========================================================

    @Test
    void jouerTourAttaqueInfligeDesDegatsEtSubitUneRiposte() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        // rapportAttaque = 10 - 5 = 5 ; rapportRiposte = 10 - 5 = 5 (pas de bonus)
        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(5, 6)).thenReturn(-4);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-3);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);
        ResultatTour resultat = tourJoue.resultat();

        assertThat(resultat.action()).isEqualTo("ATTAQUE");
        assertThat(resultat.degatsInfliges()).isEqualTo(-4);
        assertThat(resultat.degatsSubis()).isEqualTo(-3);
        assertThat(ce.getEnduranceActuelle()).isEqualTo(6);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(17);
        assertThat(combat.getAssautsLivres()).isEqualTo(1);
        assertThat(combat.isEndurancePerdue()).isTrue();
        assertThat(combat.getStatut()).isEqualTo(StatutCombat.EN_COURS);
    }

    @Test
    void jouerTourAttaqueTueLEnnemiEtPasseAuSuivantSansRiposte() {
        Ennemi ennemi1 = creerEnnemi("loup1", 5, 3);
        Ennemi ennemi2 = creerEnnemi("loup2", 5, 5);
        Chapitre chapitre = creerChapitreCombat(253, List.of(ennemi1, ennemi2), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce1 = creerCombatEnnemi(ennemi1, 0, 3);
        CombatEnnemi ce2 = creerCombatEnnemi(ennemi2, 1, 5);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce1, ce2));
        stuberCombatExistant(personnage, chapitre, combat);

        when(tableDeHasardService.tirerChiffre()).thenReturn(9);
        when(tableCombatService.degatsInfliges(5, 9)).thenReturn(-5);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(ce1.getEnduranceActuelle()).isZero();
        assertThat(combat.getEnnemiActifIndex()).isEqualTo(1);
        assertThat(combat.getStatut()).isEqualTo(StatutCombat.EN_COURS);
        assertThat(tourJoue.resultat().rapportRiposte()).isNull();
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
        verify(tableDeHasardService, org.mockito.Mockito.times(1)).tirerChiffre();
    }

    @Test
    void jouerTourAttaqueTueLeDernierEnnemiDeclencheLaVictoire() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 3);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 3);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        when(tableDeHasardService.tirerChiffre()).thenReturn(9);
        when(tableCombatService.degatsInfliges(5, 9)).thenReturn(-5);

        combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(combat.getStatut()).isEqualTo(StatutCombat.VICTOIRE);
        assertThat(combat.getEnnemiActif()).isNull();
    }

    @Test
    void jouerTourAttaqueConsommeLeBonusHabiliteEnAttenteEtLeRemetAZero() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        combat.setBonusHabiliteEnAttente(2);
        stuberCombatExistant(personnage, chapitre, combat);

        // rapportAttaque = (10 + bonus 2) - 5 = 7 ; rapportRiposte SANS bonus = 10 - 5 = 5
        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(7, 6)).thenReturn(-2);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-2);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(tourJoue.resultat().rapportAttaque()).isEqualTo(7);
        assertThat(tourJoue.resultat().rapportRiposte()).isEqualTo(5);
        assertThat(combat.getBonusHabiliteEnAttente()).isZero();
    }

    @Test
    void jouerTourAttaqueAppliqueLeBonusPuissancePsychiqueSiL_ennemiNeResistePas() {
        Discipline puissancePsychique = new Discipline();
        puissancePsychique.setId(IdDiscipline.PUISSANCE_PSYCHIQUE);

        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        personnage.setDisciplines(List.of(puissancePsychique));
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        // rapportAttaque = (10 + 2 bonus Kai) - 5 = 7
        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(7, 6)).thenReturn(-1);
        when(tableCombatService.degatsSubis(7, 4)).thenReturn(-1);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(tourJoue.resultat().rapportAttaque()).isEqualTo(7);
    }

    @Test
    void jouerTourAttaqueN_appliquePasLeBonusPuissancePsychiqueSiL_ennemiResiste() {
        Discipline puissancePsychique = new Discipline();
        puissancePsychique.setId(IdDiscipline.PUISSANCE_PSYCHIQUE);

        Ennemi ennemi = creerEnnemi("gluatre", 5, 10);
        ennemi.setResistances(List.of(puissancePsychique));
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        personnage.setDisciplines(List.of(puissancePsychique));
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        // Pas de bonus : rapportAttaque = 10 - 5 = 5
        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(5, 6)).thenReturn(-1);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-1);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(tourJoue.resultat().rapportAttaque()).isEqualTo(5);
    }

    // =========================================================
    // jouerTour - DEFENSE
    // =========================================================

    @Test
    void jouerTourDefenseReduitLesDegatsEtGardeLeBonusPourLaProchaineAttaque() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.reductionDefense(6)).thenReturn(50);
        when(tableCombatService.bonusHabilite(6)).thenReturn(1);
        // rapportRiposte = (10 + bonus 1) - 5 = 6
        when(tableCombatService.degatsSubis(6, 4)).thenReturn(-6);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.DEFENSE, null);
        ResultatTour resultat = tourJoue.resultat();

        assertThat(resultat.action()).isEqualTo("DEFENSE");
        assertThat(resultat.degatsSubisBruts()).isEqualTo(-6);
        assertThat(resultat.degatsSubis()).isEqualTo(-3); // -6 * (100-50)/100
        assertThat(resultat.reductionPourcent()).isEqualTo(50);
        assertThat(resultat.bonusHabiliteObtenu()).isEqualTo(1);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(17);
        // Le bonus reste disponible pour la PROCHAINE attaque.
        assertThat(combat.getBonusHabiliteEnAttente()).isEqualTo(1);
    }

    // =========================================================
    // jouerTour - OBJET
    // =========================================================

    @Test
    void jouerTourObjetEchoueSiAucunObjetFourni() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        Combat combat = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(ennemi, 0, 10)));
        stuberCombatExistant(personnage, chapitre, combat);

        assertThatThrownBy(() -> combatService.jouerTour(personnage, ActionCombat.OBJET, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objet est requis");

        verify(objetService, never()).appliquerEffetsConsommation(any(), any());
    }

    @Test
    void jouerTourObjetEchoueSiLaCategorieNePermetPasLaConsommation() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        Combat combat = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(ennemi, 0, 10)));
        stuberCombatExistant(personnage, chapitre, combat);

        Objet arme = new Objet();
        arme.setId("glaive");
        arme.setCategorie(CategorieObjet.ARME);

        assertThatThrownBy(() -> combatService.jouerTour(personnage, ActionCombat.OBJET, arme))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("glaive");

        verify(objetService, never()).appliquerEffetsConsommation(any(), any());
    }

    @Test
    void jouerTourObjetAppliqueL_effetPuisRetireL_objetEtSubitUneRiposte() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        Objet potion = new Objet();
        potion.setId("potion_de_soin");
        potion.setCategorie(CategorieObjet.OBJET);

        when(tableDeHasardService.tirerChiffre()).thenReturn(4);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-2);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.OBJET, potion);

        verify(objetService).appliquerEffetsConsommation(personnage, potion);
        verify(inventaireService).retirerObjet(personnage, potion, 1);
        assertThat(tourJoue.resultat().action()).isEqualTo("OBJET");
        assertThat(combat.getAssautsLivres()).isEqualTo(1);
    }

    // =========================================================
    // jouerTour - FUITE
    // =========================================================

    @Test
    void jouerTourFuiteEchoueSiLeSeuilN_estPasAtteint() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Lien lienFuite = creerLienAvecCondition(TypeCondition.FUITE, "3");
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), List.of(lienFuite));
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        Combat combat = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(ennemi, 0, 10)));
        combat.setAssautsLivres(1);
        stuberCombatExistant(personnage, chapitre, combat);

        assertThatThrownBy(() -> combatService.jouerTour(personnage, ActionCombat.FUITE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fuite");

        assertThat(combat.getStatut()).isEqualTo(StatutCombat.EN_COURS);
    }

    @Test
    void jouerTourFuiteReussitSansRiposteQuandLeSeuilEstAtteint() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Lien lienFuite = creerLienAvecCondition(TypeCondition.FUITE, "3");
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), List.of(lienFuite));
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        Combat combat = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(ennemi, 0, 10)));
        combat.setAssautsLivres(3);
        stuberCombatExistant(personnage, chapitre, combat);

        TourJoue tourJoue = combatService.jouerTour(personnage, ActionCombat.FUITE, null);

        assertThat(combat.getStatut()).isEqualTo(StatutCombat.FUITE);
        assertThat(tourJoue.resultat().action()).isEqualTo("FUITE");
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(20);
        verify(tableDeHasardService, never()).tirerChiffre();
    }

    // =========================================================
    // jouerTour - garde-fous generaux
    // =========================================================

    @Test
    void jouerTourEchoueSiLeCombatEstDejaTermine() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        Combat combat = creerCombatEnCours(chapitre, List.of(creerCombatEnnemi(ennemi, 0, 0)));
        combat.setStatut(StatutCombat.VICTOIRE);
        stuberCombatExistant(personnage, chapitre, combat);

        assertThatThrownBy(() -> combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VICTOIRE");
    }

    @Test
    void jouerTourEchoueSiLePersonnageEstMort() {
        Personnage personnage = creerPersonnage(10, 0, null);
        personnage.setMort(true);

        assertThatThrownBy(() -> combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(combatRepository, never()).findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(any(), any());
    }

    @Test
    void jouerTourPasseLeCombatEnDefaiteQuandL_enduranceDuJoueurTombeAZero() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Chapitre chapitre = creerChapitreCombat(17, List.of(ennemi), null);
        Personnage personnage = creerPersonnage(10, 3, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(5, 6)).thenReturn(-1);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-5);

        combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(personnage.getEnduranceActuelle()).isZero();
        assertThat(combat.getStatut()).isEqualTo(StatutCombat.DEFAITE);
    }

    @Test
    void jouerTourPasseLeCombatEnInterrompuQuandLeSeuilAssautEchecEstAtteint() {
        Ennemi ennemi = creerEnnemi("kraan", 5, 10);
        Lien lienEchec = creerLienAvecCondition(TypeCondition.ASSAUT_ECHEC, "1");
        Chapitre chapitre = creerChapitreCombat(231, List.of(ennemi), List.of(lienEchec));
        Personnage personnage = creerPersonnage(10, 20, chapitre);
        CombatEnnemi ce = creerCombatEnnemi(ennemi, 0, 10);
        Combat combat = creerCombatEnCours(chapitre, List.of(ce));
        stuberCombatExistant(personnage, chapitre, combat);

        // L'ennemi (10 HP) et le joueur (20 HP) survivent tous les deux a ce tour.
        when(tableDeHasardService.tirerChiffre()).thenReturn(6, 4);
        when(tableCombatService.degatsInfliges(5, 6)).thenReturn(-1);
        when(tableCombatService.degatsSubis(5, 4)).thenReturn(-1);

        combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null);

        assertThat(combat.getAssautsLivres()).isEqualTo(1);
        assertThat(combat.getStatut()).isEqualTo(StatutCombat.INTERROMPU);
    }

    // =========================================================
    // peutFuir (methode publique, utilisee aussi par CombatMapper)
    // =========================================================

    @Test
    void peutFuirRenvoieTrueQuandLeSeuilDeLaConditionFuiteEstAtteint() {
        Lien lienFuite = creerLienAvecCondition(TypeCondition.FUITE, "3");
        Chapitre chapitre = creerChapitreCombat(17, List.of(), List.of(lienFuite));
        Combat combat = new Combat();
        combat.setAssautsLivres(3);

        assertThat(combatService.peutFuir(chapitre, combat)).isTrue();
    }

    @Test
    void peutFuirRenvoieFalseSiLeSeuilN_estPasEncoreAtteint() {
        Lien lienFuite = creerLienAvecCondition(TypeCondition.FUITE, "3");
        Chapitre chapitre = creerChapitreCombat(17, List.of(), List.of(lienFuite));
        Combat combat = new Combat();
        combat.setAssautsLivres(2);

        assertThat(combatService.peutFuir(chapitre, combat)).isFalse();
    }

    @Test
    void peutFuirRenvoieFalseSiAucunLienNAUneConditionFuite() {
        Lien lienSansFuite = creerLienAvecCondition(TypeCondition.HASARD, "1");
        Chapitre chapitre = creerChapitreCombat(17, List.of(), List.of(lienSansFuite));
        Combat combat = new Combat();
        combat.setAssautsLivres(99);

        assertThat(combatService.peutFuir(chapitre, combat)).isFalse();
    }
}