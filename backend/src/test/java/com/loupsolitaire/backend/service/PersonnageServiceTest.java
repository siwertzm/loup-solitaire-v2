package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.ObjetChapitreRamasse;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.ObjetChapitreRamasseRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ResultatAjout;

@ExtendWith(MockitoExtension.class)
class PersonnageServiceTest {

    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private DisciplineRepository disciplineRepository;
    @Mock
    private ObjetRepository objetRepository;
    @Mock
    private ChapitreRepository chapitreRepository;
    @Mock
    private TableDeHasardService tableDeHasardService;
    @Mock
    private InventaireService inventaireService;
    @Mock
    private ConditionService conditionService;
    @Mock
    private EffetChapitreService effetChapitreService;
    @Mock
    private CombatRepository combatRepository;
    @Mock
    private ObjetChapitreRamasseRepository objetChapitreRamasseRepository;

    @InjectMocks
    private PersonnageService personnageService;

    private Utilisateur utilisateur;
    private Chapitre chapitre0;

    // Memes instances reutilisees pour le stubbing ET la verification : Objet
    // n'a pas d'equals()/hashCode() personnalise, donc deux instances avec le
    // meme id ne sont PAS egales pour Mockito (eq() compare par reference ici).
    private final Map<String, Objet> objetsParId = new HashMap<>();

    private static final List<IdDiscipline> CINQ_DISCIPLINES_SANS_MAITRISE = List.of(
            IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE, IdDiscipline.SIXIEME_SENS,
            IdDiscipline.ORIENTATION, IdDiscipline.GUERISON
    );

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        chapitre0 = new Chapitre();
        chapitre0.setId(0);

        for (IdDiscipline id : IdDiscipline.values()) {
            Discipline d = new Discipline();
            d.setId(id);
            lenient().when(disciplineRepository.findById(id)).thenReturn(Optional.of(d));
        }

        lenient().when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        lenient().when(personnageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Par defaut, un lien est considere disponible : sur un mock,
        // Mockito renvoie false par defaut pour un booleen non stubbe, ce
        // qui bloquerait silencieusement TOUS les tests de succes de
        // avancerVersChapitre avec "conditions non remplies". Les tests
        // qui veulent specifiquement un lien indisponible ecrasent ce
        // defaut au cas par cas.
        lenient().when(conditionService.estLienDisponible(any(), any())).thenReturn(true);

        for (String id : List.of("hache", "repas", "carte", "or", "glaive", "epee", "casque",
                "cotte_de_mailles", "masse", "potion_de_soin", "baton", "lance", "coin")) {
            Objet objet = new Objet();
            objet.setId(id);
            objetsParId.put(id, objet);
            lenient().when(objetRepository.findById(id)).thenReturn(Optional.of(objet));
        }
        lenient().when(inventaireService.ajouterObjet(any(), any(), anyInt()))
                .thenAnswer(inv -> new ResultatAjout(inv.getArgument(1), inv.getArgument(2), inv.getArgument(2), List.of()));
    }

    private InventaireItem creerLigneCoin(int quantite) {
        InventaireItem ligne = new InventaireItem();
        ligne.setObjet(objetsParId.get("coin"));
        ligne.setQuantite(quantite);
        return ligne;
    }

    @Test
    void calculeHabiliteEtEnduranceSelonLesTirages() {
        // hasardHabilite=7, hasardEndurance=3 passes en parametres ; ordre des
        // tirages restants (tableDeHasardService) : tirage hasard initial, or de depart, objet de depart
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 7, 3);

        assertThat(personnage.getHabiliteBase()).isEqualTo(17);
        assertThat(personnage.getHabilite()).isEqualTo(17);
        assertThat(personnage.getEnduranceMax()).isEqualTo(23);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(23);
    }

    @Test
    void assigneExactementLesCinqDisciplinesChoisiesEtLeChapitreDeDepart() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        assertThat(personnage.getDisciplines()).hasSize(5);
        assertThat(personnage.getChapitreActuel()).isEqualTo(chapitre0);
        assertThat(personnage.getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void fixeUnTirageHasardDesLaCreation() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(8, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        assertThat(personnage.getDernierTirageHasard()).isEqualTo(8);
    }

    @Test
    void equipeLeMaterielDeBaseFixe() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("hache")), eq(1));
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("repas")), eq(1));
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("carte")), eq(1));
    }

    @Test
    void nAjouteAucunOrSiLeTirageEstZero() {
        // tirage hasard initial=0, or=0, objet depart=2 (casque)
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 2);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        verify(inventaireService, never()).ajouterObjet(any(), eq(objetsParId.get("or")), anyInt());
    }

    @Test
    void ajouteLOrDeDepartSiLeTirageEstPositif() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 6, 2);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("or")), eq(6));
    }

    @Test
    void appliqueLaTableDeTirageDeLObjetDeDepart() {
        // tirage objet de depart = 3 -> 2 Repas
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 3);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        // 1 (equipement fixe) + 2 (objet de depart, meme id "repas") -> deux appels distincts
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("repas")), eq(1));
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("repas")), eq(2));
    }

    @Test
    void tireUneArmeMaitriseeSiLaDisciplineEstChoisie() {
        List<IdDiscipline> avecMaitrise = List.of(
                IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE, IdDiscipline.SIXIEME_SENS,
                IdDiscipline.ORIENTATION, IdDiscipline.MAITRISE_ARMES
        );
        List<Objet> armes = List.of(objetsParId.get("hache"), objetsParId.get("glaive"));
        when(objetRepository.findByCategorie(CategorieObjet.ARME)).thenReturn(armes);
        when(tableDeHasardService.tirerParmi(armes)).thenReturn(armes.get(1));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", avecMaitrise, 0, 0);

        assertThat(personnage.getArmeMaitrisee()).isEqualTo(armes.get(1));
    }

    @Test
    void neTireAucuneArmeMaitriseeSiLaDisciplineNestPasChoisie() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0);

        assertThat(personnage.getArmeMaitrisee()).isNull();
        verify(objetRepository, never()).findByCategorie(any());
    }

    @Test
    void refuseUnNombreDeDisciplinesDifferentDeCinq() {
        assertThatThrownBy(() -> personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", List.of(IdDiscipline.CAMOUFLAGE), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDesDisciplinesEnDouble() {
        List<IdDiscipline> avecDoublon = List.of(
                IdDiscipline.CAMOUFLAGE, IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE,
                IdDiscipline.SIXIEME_SENS, IdDiscipline.ORIENTATION
        );

        assertThatThrownBy(() -> personnageService.creerPersonnage(utilisateur, "Loup Solitaire", avecDoublon, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void echoueSiLeChapitreDeDepartEstIntrouvable() {
        when(chapitreRepository.findById(0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE, 0, 0))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    // =========================================================
    // Reinitialisation de l'habilite temporaire (changement de chapitre)
    // =========================================================

    @Test
    void reinitialiseHabiliteTempAZeroEtSauvegarde() {
        Personnage p = new Personnage();
        p.setHabiliteTemp(2);

        personnageService.reinitialiserHabiliteTemp(p);

        assertThat(p.getHabiliteTemp()).isEqualTo(0);
        verify(personnageRepository).save(p);
    }

    @Test
    void neSauvegardeRienSiHabiliteTempEstDejaAZero() {
        Personnage p = new Personnage();
        p.setHabiliteTemp(0);

        personnageService.reinitialiserHabiliteTemp(p);

        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // Navigation entre chapitres
    // =========================================================

    private com.loupsolitaire.backend.model.Lien creerLien(com.loupsolitaire.backend.model.Chapitre cible,
                                                             com.loupsolitaire.backend.model.Cond... conditions) {
        com.loupsolitaire.backend.model.Lien lien = new com.loupsolitaire.backend.model.Lien();
        lien.setChapitreCible(cible);
        lien.setConditions(List.of(conditions));
        return lien;
    }

    @Test
    void avanceVersLeChapitreCibleEtMetAJourPrecedentEtActuel() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setHabiliteTemp(3);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(9);

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getChapitrePrecedent()).isEqualTo(chapitre0);
        assertThat(p.getChapitreActuel()).isEqualTo(chapitre1);
        assertThat(p.getHabiliteTemp()).isEqualTo(0);
        // Nouveau tirage fige des l'arrivee sur le chapitre, pas re-tire au
        // prochain GET /chapitre.
        assertThat(p.getDernierTirageHasard()).isEqualTo(9);
        verify(personnageRepository, atLeastOnce()).save(p);
    }

    @Test
    void declencheLEffetRepasSiLeNouveauChapitreEnAUn() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        com.loupsolitaire.backend.model.Effet effetRepas = new com.loupsolitaire.backend.model.Effet();
        effetRepas.setType(com.loupsolitaire.backend.model.enums.TypeEffet.REPAS);
        chapitre1.setEffets(List.of(effetRepas));
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(effetChapitreService).appliquerEffetRepas(p);
    }

    @Test
    void neDeclencheAucunEffetSiLeNouveauChapitreNenAPas() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre1.setEffets(List.of());
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(effetChapitreService, never()).appliquerEffetRepas(any());
    }

    @Test
    void declencheLEffetHabiliteSiLeNouveauChapitreEnAUn() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        com.loupsolitaire.backend.model.Effet effetHabilite = new com.loupsolitaire.backend.model.Effet();
        effetHabilite.setType(com.loupsolitaire.backend.model.enums.TypeEffet.HABILETE);
        effetHabilite.setValeur(-2);
        effetHabilite.setConditions(List.of());
        chapitre1.setEffets(List.of(effetHabilite));
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(effetChapitreService).appliquerEffetHabilite(p, effetHabilite);
    }

    @Test
    void declencheLEffetEnduranceSiLeNouveauChapitreEnAUn() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        com.loupsolitaire.backend.model.Effet effetEndurance = new com.loupsolitaire.backend.model.Effet();
        effetEndurance.setType(com.loupsolitaire.backend.model.enums.TypeEffet.ENDURANCE);
        effetEndurance.setValeur(-2);
        effetEndurance.setConditions(List.of());
        chapitre1.setEffets(List.of(effetEndurance));
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(effetChapitreService).appliquerEffetEndurance(p, effetEndurance);
    }

    @Test
    void declencheLEffetVolSiLeNouveauChapitreEnAUn() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        com.loupsolitaire.backend.model.Effet effetVol = new com.loupsolitaire.backend.model.Effet();
        effetVol.setType(com.loupsolitaire.backend.model.enums.TypeEffet.VOL);
        effetVol.setValeur(10);
        effetVol.setConditions(List.of());
        chapitre1.setEffets(List.of(effetVol));
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(effetChapitreService).appliquerEffetVol(p, effetVol);
    }

    @Test
    void refuseDAvancerSiUnVolEstEnAttente() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setVolEnAttente(com.loupsolitaire.backend.model.enums.PorteeVol.TOUT);

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 1))
                .isInstanceOf(IllegalArgumentException.class);

        // Aucun appel au chapitre ne doit meme etre tente.
        verify(chapitreRepository, never()).findById(any());
    }

    @Test
    void refuseDAvancerVersUnChapitreSansLienDepuisLActuel() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        chapitre0.setLiens(List.of());

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 42))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDAvancerSiLesConditionsDuLienNeSontPasRemplies() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        com.loupsolitaire.backend.model.Cond condDiscipline = new com.loupsolitaire.backend.model.Cond();
        condDiscipline.setType(com.loupsolitaire.backend.model.enums.TypeCondition.DISCIPLINE);
        condDiscipline.setTargetId("chasse");
        com.loupsolitaire.backend.model.Lien lien = creerLien(chapitre1, condDiscipline);
        chapitre0.setLiens(List.of(lien));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        // PersonnageService appelle conditionService.estLienDisponible(...),
        // pas estDisponible(...) directement : conditionService est ici un
        // mock, donc stubber estDisponible() n'a aucun effet sur ce que
        // renvoie estLienDisponible() (contrairement au vrai service, ou
        // l'un delegue a l'autre). Il faut ecraser le defaut "true" pose
        // dans setUp() directement sur estLienDisponible().
        when(conditionService.estLienDisponible(lien, p)).thenReturn(false);

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(p.getChapitreActuel()).isEqualTo(chapitre0); // inchange
    }

    @Test
    void refuseDAvancerSiLePersonnageEstMort() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setMort(true);

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(chapitreRepository, never()).findById(any());
    }

    // =========================================================
    // Sortie d'un chapitre de combat : bloquee tant que non resolu
    // =========================================================

    @Test
    void refuseDeQuitterUnChapitreDeCombatNonResolu() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        chapitre0.setCombat(true);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.EN_COURS);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 0))
                .thenReturn(Optional.of(combat));

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(p.getChapitreActuel()).isEqualTo(chapitre0);
    }

    @Test
    void refuseDeQuitterUnChapitreDeCombatSiAucunCombatNAJamaisEteEngage() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        chapitre0.setCombat(true);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 0))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personnageService.avancerVersChapitre(p, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void autoriseDeQuitterUnChapitreDeCombatResoluParVictoire() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        chapitre0.setCombat(true);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.VICTOIRE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 0))
                .thenReturn(Optional.of(combat));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getChapitreActuel()).isEqualTo(chapitre1);
    }

    // =========================================================
    // Arrivee sur un chapitre de combat : un ancien combat resolu est
    // supprime pour permettre un affrontement entierement neuf
    // =========================================================

    @Test
    void supprimeUnCombatDejaResoluALArriveeSurUnNouveauChapitreDeCombat() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre1.setCombat(true);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        Combat ancienCombat = new Combat();
        ancienCombat.setStatut(StatutCombat.VICTOIRE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 1))
                .thenReturn(Optional.of(ancienCombat));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(combatRepository).delete(ancienCombat);
    }

    @Test
    void neSupprimeRienSiLeCombatDuNouveauChapitreEstEncoreEnCours() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre1.setCombat(true);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        Combat combatEnCours = new Combat();
        combatEnCours.setStatut(StatutCombat.EN_COURS);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 1))
                .thenReturn(Optional.of(combatEnCours));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(combatRepository, never()).delete(any());
    }

    // =========================================================
    // Discipline Kai Guerison : +1 ENDURANCE a l'arrivee sur un chapitre
    // SANS combat, jamais sur un chapitre de combat
    // =========================================================

    @Test
    void laDisciplineGuerisonAugmenteLEnduranceDUnPointSurUnChapitreSansCombat() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(15);
        Discipline guerison = new Discipline();
        guerison.setId(IdDiscipline.GUERISON);
        p.setDisciplines(List.of(guerison));

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getEnduranceActuelle()).isEqualTo(16);
    }

    @Test
    void laGuerisonNeDepassePasLePlafondDEndurance() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(20);
        Discipline guerison = new Discipline();
        guerison.setId(IdDiscipline.GUERISON);
        p.setDisciplines(List.of(guerison));

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getEnduranceActuelle()).isEqualTo(20);
    }

    @Test
    void laGuerisonNeSAppliquePasSurUnChapitreDeCombat() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(15);
        Discipline guerison = new Discipline();
        guerison.setId(IdDiscipline.GUERISON);
        p.setDisciplines(List.of(guerison));

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre1.setCombat(true);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);
        // nouveauChapitre est un combat : le service verifie aussi s'il faut
        // purger un ancien combat resolu.
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 1))
                .thenReturn(Optional.empty());

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getEnduranceActuelle()).isEqualTo(15); // inchangee : c'est un combat
    }

    @Test
    void laGuerisonNeSAppliquePasSansLaDiscipline() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(15);
        p.setDisciplines(List.of());

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        assertThat(p.getEnduranceActuelle()).isEqualTo(15);
    }

    // =========================================================
    // revenirApresDefaite : retour payant (1 coin) au chapitre precedent
    // apres une DEFAITE en combat
    // =========================================================

    @Test
    void revenirApresDefaiteRestaureLEnduranceEtRevientAuChapitrePrecedent() {
        Personnage p = new Personnage();
        Chapitre chapitreCombat = new Chapitre();
        chapitreCombat.setId(5);
        Chapitre chapitrePrecedent = new Chapitre();
        chapitrePrecedent.setId(4);
        p.setChapitreActuel(chapitreCombat);
        p.setChapitrePrecedent(chapitrePrecedent);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(0);

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.DEFAITE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 5))
                .thenReturn(Optional.of(combat));
        when(inventaireService.listerInventaire(p)).thenReturn(List.of(creerLigneCoin(999)));
        when(tableDeHasardService.tirerChiffre()).thenReturn(4);

        personnageService.revenirApresDefaite(p);

        assertThat(p.getChapitreActuel()).isEqualTo(chapitrePrecedent);
        assertThat(p.getEnduranceActuelle()).isEqualTo(20);
        assertThat(p.getDernierTirageHasard()).isEqualTo(4);
        verify(personnageRepository, atLeastOnce()).save(p);
    }

    @Test
    void revenirApresDefaiteEchoueSiAucunCombatTrouve() {
        Personnage p = new Personnage();
        Chapitre chapitreCombat = new Chapitre();
        chapitreCombat.setId(5);
        p.setChapitreActuel(chapitreCombat);

        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 5))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personnageService.revenirApresDefaite(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revenirApresDefaiteEchoueSiLeCombatNEstPasPerdu() {
        Personnage p = new Personnage();
        Chapitre chapitreCombat = new Chapitre();
        chapitreCombat.setId(5);
        p.setChapitreActuel(chapitreCombat);

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.VICTOIRE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 5))
                .thenReturn(Optional.of(combat));

        assertThatThrownBy(() -> personnageService.revenirApresDefaite(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revenirApresDefaiteEchoueSiAucunChapitrePrecedent() {
        Personnage p = new Personnage();
        Chapitre chapitreCombat = new Chapitre();
        chapitreCombat.setId(5);
        p.setChapitreActuel(chapitreCombat);
        p.setChapitrePrecedent(null);

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.DEFAITE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 5))
                .thenReturn(Optional.of(combat));

        assertThatThrownBy(() -> personnageService.revenirApresDefaite(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revenirApresDefaiteEchoueSansPieceCoin() {
        Personnage p = new Personnage();
        Chapitre chapitreCombat = new Chapitre();
        chapitreCombat.setId(5);
        Chapitre chapitrePrecedent = new Chapitre();
        chapitrePrecedent.setId(4);
        p.setChapitreActuel(chapitreCombat);
        p.setChapitrePrecedent(chapitrePrecedent);

        Combat combat = new Combat();
        combat.setStatut(StatutCombat.DEFAITE);
        when(combatRepository.findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(p, 5))
                .thenReturn(Optional.of(combat));
        when(inventaireService.listerInventaire(p)).thenReturn(List.of());

        assertThatThrownBy(() -> personnageService.revenirApresDefaite(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================
    // ressusciter : retour a la vie payant (1 coin) apres une mort hors
    // combat, sans changer de chapitre
    // =========================================================

    @Test
    void ressusciterRestaureLEnduranceEtRepasseMortAFalse() {
        Personnage p = new Personnage();
        p.setMort(true);
        p.setEnduranceMax(20);
        p.setEnduranceActuelle(0);

        when(inventaireService.listerInventaire(p)).thenReturn(List.of(creerLigneCoin(999)));

        personnageService.ressusciter(p);

        assertThat(p.isMort()).isFalse();
        assertThat(p.getEnduranceActuelle()).isEqualTo(20);
        verify(personnageRepository).save(p);
    }

    @Test
    void ressusciterEchoueSiLePersonnageNEstPasMort() {
        Personnage p = new Personnage();
        p.setMort(false);

        assertThatThrownBy(() -> personnageService.ressusciter(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ressusciterEchoueSansPieceCoin() {
        Personnage p = new Personnage();
        p.setMort(true);
        when(inventaireService.listerInventaire(p)).thenReturn(List.of());

        assertThatThrownBy(() -> personnageService.ressusciter(p))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(p.isMort()).isTrue(); // inchange
        verify(personnageRepository, never()).save(any());
    }

    // =========================================================
    // Echange volontaire (ex. chapitre 307 : Marteau de Guerre)
    // =========================================================

    private com.loupsolitaire.backend.model.Objet creerObjetArme(String id) {
        com.loupsolitaire.backend.model.Objet objet = new com.loupsolitaire.backend.model.Objet();
        objet.setId(id);
        objet.setCategorie(CategorieObjet.ARME);
        return objet;
    }

    private com.loupsolitaire.backend.model.Effet creerEffetEchange(String targetId) {
        com.loupsolitaire.backend.model.Effet effet = new com.loupsolitaire.backend.model.Effet();
        effet.setType(com.loupsolitaire.backend.model.enums.TypeEffet.ECHANGE);
        effet.setValeur(1);
        com.loupsolitaire.backend.model.Cond cond = new com.loupsolitaire.backend.model.Cond();
        cond.setType(com.loupsolitaire.backend.model.enums.TypeCondition.ARME);
        cond.setTargetId(targetId);
        effet.setConditions(List.of(cond));
        return effet;
    }

    @Test
    void refuseLEchangeSiLePersonnageEstMort() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setMort(true);

        Objet hache = creerObjetArme("hache");
        Objet marteau = creerObjetArme("marteau");

        assertThatThrownBy(() -> personnageService.echangerObjet(p, hache, marteau))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(chapitreRepository, never()).findById(any());
    }

    @Test
    void refuseLEchangeSiLesCategoriesNeCorrespondentPas() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Objet hache = creerObjetArme("hache");
        Objet repas = new Objet();
        repas.setId("repas");
        repas.setCategorie(CategorieObjet.REPAS);

        assertThatThrownBy(() -> personnageService.echangerObjet(p, hache, repas))
                .isInstanceOf(IllegalArgumentException.class);

        // La verification de categorie se fait avant meme d'aller chercher
        // le chapitre : aucun appel inutile.
        verify(chapitreRepository, never()).findById(any());
        verify(inventaireService, never()).remplacerObjet(any(), any(), anyInt(), any(), anyInt());
    }

    @Test
    void echangeAutoriseSiLeChapitreActuelProposeCetEchangePrecis() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Objet hache = creerObjetArme("hache");
        Objet marteau = creerObjetArme("marteau");
        chapitre0.setEffets(List.of(creerEffetEchange("marteau")));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        InventaireItem ligneHache = new InventaireItem();
        ligneHache.setObjet(hache);
        ligneHache.setQuantite(1);
        when(inventaireService.listerInventaire(p)).thenReturn(List.of(ligneHache));

        personnageService.echangerObjet(p, hache, marteau);

        verify(inventaireService).remplacerObjet(p, hache, 1, marteau, 1);
    }

    @Test
    void refuseLEchangeSiLeChapitreActuelNeLeProposePas() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Objet hache = creerObjetArme("hache");
        Objet marteau = creerObjetArme("marteau");
        chapitre0.setEffets(List.of()); // aucun effet ECHANGE sur ce chapitre

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        assertThatThrownBy(() -> personnageService.echangerObjet(p, hache, marteau))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inventaireService, never()).remplacerObjet(any(), any(), anyInt(), any(), anyInt());
    }

    @Test
    void refuseLEchangeSiLObjetProposeNeCorrespondPasATargetId() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Objet hache = creerObjetArme("hache");
        Objet epee = creerObjetArme("epee");
        // Le chapitre propose un echange pour "marteau", pas pour "epee".
        chapitre0.setEffets(List.of(creerEffetEchange("marteau")));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        assertThatThrownBy(() -> personnageService.echangerObjet(p, hache, epee))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseLEchangeSiLObjetARetirerNestPasPossede() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Objet hache = creerObjetArme("hache");
        Objet marteau = creerObjetArme("marteau");
        chapitre0.setEffets(List.of(creerEffetEchange("marteau")));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(inventaireService.listerInventaire(p)).thenReturn(List.of()); // aucune arme possedee

        assertThatThrownBy(() -> personnageService.echangerObjet(p, hache, marteau))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inventaireService, never()).remplacerObjet(any(), any(), anyInt(), any(), anyInt());
    }

    // =========================================================
    // Objets du chapitre (ObjetChap) : seuls les obligatoires s'appliquent
    // =========================================================

    private com.loupsolitaire.backend.model.ObjetChap creerObjetChap(
            com.loupsolitaire.backend.model.Objet objet, int valeur, boolean optionnel) {
        com.loupsolitaire.backend.model.ObjetChap objetChap = new com.loupsolitaire.backend.model.ObjetChap();
        objetChap.setObjet(objet);
        objetChap.setValeur(valeur);
        objetChap.setOptionnel(optionnel);
        return objetChap;
    }

    @Test
    void ajouteAutomatiquementUnObjetObligatoireAValeurPositive() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        Objet or = objetsParId.get("or");
        chapitre1.setObjets(List.of(creerObjetChap(or, 6, false)));
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);
        when(inventaireService.ajouterObjet(p, or, 6)).thenReturn(new ResultatAjout(or, 6, 6, List.of()));

        personnageService.avancerVersChapitre(p, 1);

        verify(inventaireService).ajouterObjet(p, or, 6);
    }

    @Test
    void retireAutomatiquementUnObjetObligatoireAValeurNegative() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        Objet or = objetsParId.get("or");
        chapitre1.setObjets(List.of(creerObjetChap(or, -10, false))); // paiement, ex. chapitre 262
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(inventaireService).retirerObjet(p, or, 10);
    }

    @Test
    void nAppliqueRienAutomatiquementPourUnObjetOptionnel() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);

        Chapitre chapitre1 = new Chapitre();
        chapitre1.setId(1);
        Objet repas = objetsParId.get("repas");
        chapitre1.setObjets(List.of(creerObjetChap(repas, 1, true))); // ramassage optionnel
        chapitre0.setLiens(List.of(creerLien(chapitre1)));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(tableDeHasardService.tirerChiffre()).thenReturn(0);

        personnageService.avancerVersChapitre(p, 1);

        verify(inventaireService, never()).ajouterObjet(eq(p), eq(repas), anyInt());
    }

    // =========================================================
    // Ramassage manuel securise (POST /objets/{objetId})
    // =========================================================

    @Test
    void ramasseUnObjetProposeParLeChapitreActuelUnParUn() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        Objet repas = objetsParId.get("repas");
        chapitre0.setObjets(List.of(creerObjetChap(repas, 2, true))); // "valeur=2" dans la donnee

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(inventaireService.ajouterObjet(p, repas, 1)).thenReturn(new ResultatAjout(repas, 1, 1, List.of()));

        personnageService.ramasserObjetDuChapitre(p, repas);

        // Toujours 1 par appel, jamais la "valeur" du chapitre directement.
        verify(inventaireService).ajouterObjet(p, repas, 1);
    }

    @Test
    void refuseDeRamasserUnObjetNonProposeParLeChapitreActuel() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        chapitre0.setObjets(List.of()); // rien de propose ici
        Objet hache = objetsParId.get("hache");

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));

        assertThatThrownBy(() -> personnageService.ramasserObjetDuChapitre(p, hache))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inventaireService, never()).ajouterObjet(any(), any(), anyInt());
    }

    @Test
    void refuseDeRamasserSiLePlafondDuChapitreEstDejaAtteint() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        Objet repas = objetsParId.get("repas");
        chapitre0.setObjets(List.of(creerObjetChap(repas, 2, true))); // max 2 sur ce chapitre

        ObjetChapitreRamasse dejaPris = new ObjetChapitreRamasse();
        dejaPris.setQuantite(2); // deja pris les 2 exemplaires disponibles

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(objetChapitreRamasseRepository.findByPersonnageIdAndChapitreIdAndObjetId(any(), eq(0), eq("repas")))
                .thenReturn(Optional.of(dejaPris));

        // Sans ce plafond, rien n'empechait de reprendre le meme objet a l'infini
        // sur le meme chapitre (le seul garde-fou existant etait cote client).
        assertThatThrownBy(() -> personnageService.ramasserObjetDuChapitre(p, repas))
                .isInstanceOf(IllegalArgumentException.class);

        verify(inventaireService, never()).ajouterObjet(any(), any(), anyInt());
    }

    @Test
    void completeLeManqueEnUnSeulAppelPourUnObjetObligatoire() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        Objet or = objetsParId.get("or");
        chapitre0.setObjets(List.of(creerObjetChap(or, 6, false))); // obligatoire, valeur=6

        ObjetChapitreRamasse dejaApplique = new ObjetChapitreRamasse();
        dejaApplique.setQuantite(2); // seuls 2/6 avaient pu etre appliques a l'arrivee (categorie pleine alors)

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre0));
        when(objetChapitreRamasseRepository.findByPersonnageIdAndChapitreIdAndObjetId(any(), eq(0), eq("or")))
                .thenReturn(Optional.of(dejaApplique));
        when(inventaireService.ajouterObjet(p, or, 4)).thenReturn(new ResultatAjout(or, 4, 4, List.of()));

        personnageService.ramasserObjetDuChapitre(p, or);

        // Complete tout le manque (6-2=4) en un seul appel, contrairement a un
        // objet optionnel qui ajoute toujours 1 exemplaire a la fois.
        verify(inventaireService).ajouterObjet(p, or, 4);
    }

    @Test
    void refuseDeRamasserSiLePersonnageEstMort() {
        Personnage p = new Personnage();
        p.setChapitreActuel(chapitre0);
        p.setMort(true);

        assertThatThrownBy(() -> personnageService.ramasserObjetDuChapitre(p, objetsParId.get("hache")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mort");

        verify(chapitreRepository, never()).findById(any());
    }
}