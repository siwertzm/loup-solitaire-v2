package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.EnnemiRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;

@ExtendWith(MockitoExtension.class)
class GameDataLoaderTest {

    @Mock
    private DisciplineRepository disciplineRepository;
    @Mock
    private EnnemiRepository ennemiRepository;
    @Mock
    private ObjetRepository objetRepository;
    @Mock
    private ChapitreRepository chapitreRepository;

    private GameDataLoader creerLoader() {
        return new GameDataLoader(disciplineRepository, ennemiRepository, objetRepository, chapitreRepository);
    }

    // =========================================================
    // Disciplines
    // =========================================================

    @Test
    void chargeLesDixDisciplinesDepuisLeFichierJson() throws Exception {
        when(disciplineRepository.count()).thenReturn(0L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(1L);

        creerLoader().run(null);

        ArgumentCaptor<List<Discipline>> captor = ArgumentCaptor.forClass(List.class);
        verify(disciplineRepository).saveAll(captor.capture());

        List<Discipline> disciplines = captor.getValue();
        assertThat(disciplines).hasSize(10);
        assertThat(disciplines)
                .extracting(Discipline::getId)
                .contains(IdDiscipline.CAMOUFLAGE, IdDiscipline.MAITRISE_ARMES, IdDiscipline.SIXIEME_SENS);
    }

    @Test
    void neRechargeRienSiLesDisciplinesSontDejaPresentes() throws Exception {
        when(disciplineRepository.count()).thenReturn(10L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(1L);

        creerLoader().run(null);

        verify(disciplineRepository, never()).saveAll(anyList());
    }

    // =========================================================
    // Ennemis
    // =========================================================

    @Test
    void chargeLesVingtCinqEnnemisEtResoutLeursResistances() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(0L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(1L);

        Discipline puissancePsychique = new Discipline();
        puissancePsychique.setId(IdDiscipline.PUISSANCE_PSYCHIQUE);
        Discipline communicationAnimale = new Discipline();
        communicationAnimale.setId(IdDiscipline.COMMUNICATION_ANIMALE);

        when(disciplineRepository.findById(IdDiscipline.PUISSANCE_PSYCHIQUE))
                .thenReturn(Optional.of(puissancePsychique));
        when(disciplineRepository.findById(IdDiscipline.COMMUNICATION_ANIMALE))
                .thenReturn(Optional.of(communicationAnimale));

        creerLoader().run(null);

        ArgumentCaptor<List<Ennemi>> captor = ArgumentCaptor.forClass(List.class);
        verify(ennemiRepository).saveAll(captor.capture());

        List<Ennemi> ennemis = captor.getValue();
        assertThat(ennemis).hasSize(25);

        Ennemi gluatre = ennemis.stream().filter(e -> e.getId().equals("gluatre")).findFirst().orElseThrow();
        assertThat(gluatre.getResistances())
                .extracting(Discipline::getId)
                .containsExactlyInAnyOrder(IdDiscipline.PUISSANCE_PSYCHIQUE, IdDiscipline.COMMUNICATION_ANIMALE);
    }

    @Test
    void neRechargeRienSiLesEnnemisSontDejaPresents() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(25L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(1L);

        creerLoader().run(null);

        verify(ennemiRepository, never()).saveAll(anyList());
    }

    @Test
    void echoueSiUneResistanceReferenceUneDisciplineInconnueEnBase() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(0L);
        when(disciplineRepository.findById(IdDiscipline.PUISSANCE_PSYCHIQUE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creerLoader().run(null))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    // =========================================================
    // Objets
    // =========================================================

    @Test
    void chargeLesVingtSeptObjetsAvecLeursEffets() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(0L);
        when(chapitreRepository.count()).thenReturn(1L);

        creerLoader().run(null);

        ArgumentCaptor<List<Objet>> captor = ArgumentCaptor.forClass(List.class);
        verify(objetRepository).saveAll(captor.capture());

        List<Objet> objets = captor.getValue();
        assertThat(objets).hasSize(27);

        Objet casque = objets.stream().filter(o -> o.getId().equals("casque")).findFirst().orElseThrow();
        assertThat(casque.getCategorie()).isEqualTo(CategorieObjet.OBJETS_SPECIAUX);
        assertThat(casque.getEffets()).hasSize(1);
        Effet effetCasque = casque.getEffets().get(0);
        assertThat(effetCasque.getType()).isEqualTo(TypeEffet.ENDURANCE);
        assertThat(effetCasque.getValeur()).isEqualTo(2);

        Objet poignard = objets.stream().filter(o -> o.getId().equals("poignard")).findFirst().orElseThrow();
        assertThat(poignard.getEffets()).isEmpty();
    }

    @Test
    void neRechargeRienSiLesObjetsSontDejaPresents() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(27L);
        when(chapitreRepository.count()).thenReturn(1L);

        creerLoader().run(null);

        verify(objetRepository, never()).saveAll(anyList());
    }

    // =========================================================
    // Chapitres
    // =========================================================

    @Test
    void chargeLesTroisCentCinquanteTroisChapitresEtResoutLeursReferences() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(0L);

        // Le catalogue reel (27 objets, 25 ennemis) n'est pas charge dans ce
        // test isole : on simule des lookups toujours reussis pour n'importe
        // quel id demande, plutot que de mocker les 52 entrees une par une.
        when(ennemiRepository.findById(anyString())).thenAnswer(inv -> {
            Ennemi e = new Ennemi();
            e.setId(inv.getArgument(0));
            return Optional.of(e);
        });
        when(objetRepository.findById(anyString())).thenAnswer(inv -> {
            Objet o = new Objet();
            o.setId(inv.getArgument(0));
            return Optional.of(o);
        });

        // Capture manuelle via thenAnswer plutot que verify(times(2)) +
        // ArgumentCaptor : plus robuste ici, saveAll etant une methode a
        // parametre de type propre (<S extends T> List<S> saveAll(Iterable<S>)).
        List<List<Chapitre>> appelsCaptures = new ArrayList<>();
        when(chapitreRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<Chapitre>>any()))
                .thenAnswer(inv -> {
                    Iterable<Chapitre> arg = inv.getArgument(0);
                    List<Chapitre> copie = new ArrayList<>();
                    arg.forEach(copie::add);
                    appelsCaptures.add(copie);
                    return copie;
                });

        creerLoader().run(null);

        assertThat(appelsCaptures).hasSize(2);
        List<Chapitre> chapitresFinaux = appelsCaptures.get(appelsCaptures.size() - 1);

        assertThat(chapitresFinaux).hasSize(353);

        Chapitre chapitre0 = chapitresFinaux.stream().filter(c -> c.getId() == 0).findFirst().orElseThrow();
        assertThat(chapitre0.getLiens()).hasSize(1);
        assertThat(chapitre0.getLiens().get(0).getChapitreCible().getId()).isEqualTo(1);

        // Chapitre 53 : sa seule sortie ("352") est une fin de partie ignoree
        // au chargement -> aucun lien persiste.
        Chapitre chapitre53 = chapitresFinaux.stream().filter(c -> c.getId() == 53).findFirst().orElseThrow();
        assertThat(chapitre53.getLiens()).isEmpty();

        // Chapitre 350 : fin victorieuse du tome, seule sortie ("351")
        // egalement ignoree.
        Chapitre chapitre350 = chapitresFinaux.stream().filter(c -> c.getId() == 350).findFirst().orElseThrow();
        assertThat(chapitre350.getLiens()).isEmpty();

        // Chapitre 17 : combat contre un Kraan.
        Chapitre chapitre17 = chapitresFinaux.stream().filter(c -> c.getId() == 17).findFirst().orElseThrow();
        assertThat(chapitre17.isCombat()).isTrue();
        assertThat(chapitre17.getEnnemis()).extracting(Ennemi::getId).containsExactly("kraan");

        // Chapitre 20 : objets ramassables (repas + poignard).
        Chapitre chapitre20 = chapitresFinaux.stream().filter(c -> c.getId() == 20).findFirst().orElseThrow();
        assertThat(chapitre20.getObjets()).hasSize(2);
    }

    @Test
    void neRechargeRienSiLesChapitresSontDejaPresents() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(1L);
        when(chapitreRepository.count()).thenReturn(353L);

        creerLoader().run(null);

        verify(chapitreRepository, never()).saveAll(anyList());
    }
}