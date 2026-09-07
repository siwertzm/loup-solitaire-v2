package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.TypeEffet;
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

    private GameDataLoader creerLoader() {
        return new GameDataLoader(disciplineRepository, ennemiRepository, objetRepository);
    }

    // =========================================================
    // Disciplines
    // =========================================================

    @Test
    void chargeLesDixDisciplinesDepuisLeFichierJson() throws Exception {
        when(disciplineRepository.count()).thenReturn(0L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(1L);

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
        assertThat(effetCasque.getObjet()).isEqualTo(casque);
        assertThat(effetCasque.getConditions()).isEmpty();

        Objet poignard = objets.stream().filter(o -> o.getId().equals("poignard")).findFirst().orElseThrow();
        assertThat(poignard.getCategorie()).isEqualTo(CategorieObjet.ARME);
        assertThat(poignard.getEffets()).isEmpty();

        Objet or = objets.stream().filter(o -> o.getId().equals("or")).findFirst().orElseThrow();
        assertThat(or.getCategorie()).isEqualTo(CategorieObjet.BOURSE);
    }

    @Test
    void neRechargeRienSiLesObjetsSontDejaPresents() throws Exception {
        when(disciplineRepository.count()).thenReturn(1L);
        when(ennemiRepository.count()).thenReturn(1L);
        when(objetRepository.count()).thenReturn(27L);

        creerLoader().run(null);

        verify(objetRepository, never()).saveAll(anyList());
    }
}