package com.loupsolitaire.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.response.DisciplineResponse;

@ExtendWith(MockitoExtension.class)
class DisciplineControllerTest {

    @Mock
    private DisciplineRepository disciplineRepository;

    @InjectMocks
    private DisciplineController disciplineController;

    private static Discipline discipline(IdDiscipline id, String nom) {
        Discipline discipline = new Discipline();
        discipline.setId(id);
        discipline.setNom(nom);
        discipline.setDescription("Description de " + nom);
        return discipline;
    }

    @Test
    void listeLesDisciplinesDansLOrdreDuLivre() {
        // La base les renvoie dans un ordre quelconque.
        when(disciplineRepository.findAll()).thenReturn(List.of(
                discipline(IdDiscipline.MAITRISE_MATIERE, "Maitrise Psychique de la Matiere"),
                discipline(IdDiscipline.CAMOUFLAGE, "Camouflage"),
                discipline(IdDiscipline.GUERISON, "Guerison")));

        List<DisciplineResponse> disciplines = disciplineController.lister();

        // Ordre de l'enum IdDiscipline, identique a celui du livre.
        assertThat(disciplines).extracting(DisciplineResponse::id)
                .containsExactly("CAMOUFLAGE", "GUERISON", "MAITRISE_MATIERE");
        assertThat(disciplines.getFirst())
                .isEqualTo(new DisciplineResponse("CAMOUFLAGE", "Camouflage", "Description de Camouflage"));
    }

    @Test
    void renvoieUneListeVideSiAucuneDisciplineNEstChargee() {
        when(disciplineRepository.findAll()).thenReturn(List.of());

        assertThat(disciplineController.lister()).isEmpty();
    }
}