package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
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

        for (String id : List.of("hache", "repas", "carte", "or", "glaive", "epee", "casque",
                "cotte_de_mailles", "masse", "potion_de_soin", "baton", "lance")) {
            Objet objet = new Objet();
            objet.setId(id);
            objetsParId.put(id, objet);
            lenient().when(objetRepository.findById(id)).thenReturn(Optional.of(objet));
        }
        lenient().when(inventaireService.ajouterObjet(any(), any(), anyInt()))
                .thenAnswer(inv -> new ResultatAjout(inv.getArgument(1), inv.getArgument(2), inv.getArgument(2), List.of()));
    }

    @Test
    void calculeHabiliteEtEnduranceSelonLesTirages() {
        // ordre des tirages : habilite, endurance, or de depart, objet de depart
        when(tableDeHasardService.tirerChiffre()).thenReturn(7, 3, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        assertThat(personnage.getHabilite()).isEqualTo(17);
        assertThat(personnage.getEnduranceMax()).isEqualTo(23);
        assertThat(personnage.getEnduranceActuelle()).isEqualTo(23);
    }

    @Test
    void assigneExactementLesCinqDisciplinesChoisiesEtLeChapitreDeDepart() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        assertThat(personnage.getDisciplines()).hasSize(5);
        assertThat(personnage.getChapitreActuel()).isEqualTo(chapitre0);
        assertThat(personnage.getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void equipeLeMaterielDeBaseFixe() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 0);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("hache")), eq(1));
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("repas")), eq(1));
        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("carte")), eq(1));
    }

    @Test
    void nAjouteAucunOrSiLeTirageEstZero() {
        // habilite=0, endurance=0, or=0, objet depart=2 (casque)
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 2);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        verify(inventaireService, never()).ajouterObjet(any(), eq(objetsParId.get("or")), anyInt());
    }

    @Test
    void ajouteLOrDeDepartSiLeTirageEstPositif() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 6, 2);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        verify(inventaireService).ajouterObjet(any(), eq(objetsParId.get("or")), eq(6));
    }

    @Test
    void appliqueLaTableDeTirageDeLObjetDeDepart() {
        // tirage objet de depart = 3 -> 2 Repas
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 3);

        personnageService.creerPersonnage(utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

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
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", avecMaitrise);

        assertThat(personnage.getArmeMaitrisee()).isEqualTo(armes.get(1));
    }

    @Test
    void neTireAucuneArmeMaitriseeSiLaDisciplineNestPasChoisie() {
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0, 0, 0);

        Personnage personnage = personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE);

        assertThat(personnage.getArmeMaitrisee()).isNull();
        verify(objetRepository, never()).findByCategorie(any());
    }

    @Test
    void refuseUnNombreDeDisciplinesDifferentDeCinq() {
        assertThatThrownBy(() -> personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", List.of(IdDiscipline.CAMOUFLAGE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDesDisciplinesEnDouble() {
        List<IdDiscipline> avecDoublon = List.of(
                IdDiscipline.CAMOUFLAGE, IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE,
                IdDiscipline.SIXIEME_SENS, IdDiscipline.ORIENTATION
        );

        assertThatThrownBy(() -> personnageService.creerPersonnage(utilisateur, "Loup Solitaire", avecDoublon))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void echoueSiLeChapitreDeDepartEstIntrouvable() {
        when(chapitreRepository.findById(0)).thenReturn(Optional.empty());
        when(tableDeHasardService.tirerChiffre()).thenReturn(0, 0);

        assertThatThrownBy(() -> personnageService.creerPersonnage(
                utilisateur, "Loup Solitaire", CINQ_DISCIPLINES_SANS_MAITRISE))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }
}