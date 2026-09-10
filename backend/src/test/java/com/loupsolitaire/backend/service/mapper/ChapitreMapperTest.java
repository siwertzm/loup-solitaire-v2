package com.loupsolitaire.backend.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.ObjetChap;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.service.ConditionService;

@ExtendWith(MockitoExtension.class)
class ChapitreMapperTest {

    @Mock
    private ChapitreRepository chapitreRepository;
    @Mock
    private ConditionService conditionService;

    @InjectMocks
    private ChapitreMapper chapitreMapper;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        personnage = new Personnage();
    }

    private Chapitre creerChapitre(int id, String text, boolean combat) {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(id);
        chapitre.setText(text);
        chapitre.setCombat(combat);
        return chapitre;
    }

    @Test
    void mappeLeTexteEtLeFlagCombat() {
        Chapitre chapitre = creerChapitre(17, "Un Kraan surgit devant vous.", true);
        when(chapitreRepository.findById(17)).thenReturn(Optional.of(chapitre));

        ChapitreResponse reponse = chapitreMapper.versReponse(17, personnage);

        assertThat(reponse.id()).isEqualTo(17);
        assertThat(reponse.text()).isEqualTo("Un Kraan surgit devant vous.");
        assertThat(reponse.combat()).isTrue();
    }

    @Test
    void mappeLeTirageHasardCourantDuPersonnage() {
        personnage.setDernierTirageHasard(7);
        Chapitre chapitre = creerChapitre(17, "texte", false);
        when(chapitreRepository.findById(17)).thenReturn(Optional.of(chapitre));

        ChapitreResponse reponse = chapitreMapper.versReponse(17, personnage);

        assertThat(reponse.tirageHasard()).isEqualTo(7);
    }

    @Test
    void mappeLesEnnemis() {
        Chapitre chapitre = creerChapitre(17, "texte", true);
        Ennemi kraan = new Ennemi();
        kraan.setId("kraan");
        kraan.setNom("Kraan");
        kraan.setHabilite(18);
        kraan.setEndurance(20);
        chapitre.setEnnemis(List.of(kraan));
        when(chapitreRepository.findById(17)).thenReturn(Optional.of(chapitre));

        ChapitreResponse reponse = chapitreMapper.versReponse(17, personnage);

        assertThat(reponse.ennemis()).hasSize(1);
        assertThat(reponse.ennemis().get(0).id()).isEqualTo("kraan");
        assertThat(reponse.ennemis().get(0).habilite()).isEqualTo(18);
        assertThat(reponse.ennemis().get(0).endurance()).isEqualTo(20);
    }

    @Test
    void mappeLesEffetsAvecLeursConditions() {
        Chapitre chapitre = creerChapitre(236, "texte", false);

        Effet effet = new Effet();
        effet.setType(TypeEffet.HABILETE);
        effet.setValeur(-1);
        Cond permanent = new Cond();
        permanent.setType(TypeCondition.PERMANENT);
        effet.setConditions(List.of(permanent));
        chapitre.setEffets(List.of(effet));

        when(chapitreRepository.findById(236)).thenReturn(Optional.of(chapitre));

        ChapitreResponse reponse = chapitreMapper.versReponse(236, personnage);

        assertThat(reponse.effets()).hasSize(1);
        assertThat(reponse.effets().get(0).type()).isEqualTo("HABILETE");
        assertThat(reponse.effets().get(0).valeur()).isEqualTo(-1);
        assertThat(reponse.effets().get(0).conditions()).hasSize(1);
        assertThat(reponse.effets().get(0).conditions().get(0).type()).isEqualTo("PERMANENT");
    }

    @Test
    void mappeLesLiensAvecLeChapitreCibleEtLeursConditions() {
        Chapitre chapitre = creerChapitre(0, "texte", false);
        Chapitre cible = creerChapitre(1, "texte cible", false);

        Lien lien = new Lien();
        lien.setChapitreCible(cible);
        Cond condDiscipline = new Cond();
        condDiscipline.setType(TypeCondition.DISCIPLINE);
        condDiscipline.setTargetId("chasse");
        lien.setConditions(List.of(condDiscipline));
        chapitre.setLiens(List.of(lien));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre));
        // Le mapper appelle conditionService.estLienDisponible(lien, personnage),
        // pas estDisponible(cond, personnage) directement : conditionService
        // etant un mock, stubber estDisponible() n'a aucun effet sur ce que
        // renvoie estLienDisponible() (le mock n'execute pas la vraie logique
        // de delegation de l'un vers l'autre).
        when(conditionService.estLienDisponible(lien, personnage)).thenReturn(true);

        ChapitreResponse reponse = chapitreMapper.versReponse(0, personnage);

        assertThat(reponse.liens()).hasSize(1);
        assertThat(reponse.liens().get(0).chapitreCibleId()).isEqualTo(1);
        assertThat(reponse.liens().get(0).disponible()).isTrue();
        assertThat(reponse.liens().get(0).conditions()).hasSize(1);
        assertThat(reponse.liens().get(0).conditions().get(0).type()).isEqualTo("DISCIPLINE");
        assertThat(reponse.liens().get(0).conditions().get(0).targetId()).isEqualTo("chasse");
    }

    @Test
    void marqueUnLienIndisponibleSiUneConditionEchoue() {
        Chapitre chapitre = creerChapitre(0, "texte", false);
        Chapitre cible = creerChapitre(1, "texte cible", false);

        Lien lien = new Lien();
        lien.setChapitreCible(cible);
        Cond condDiscipline = new Cond();
        condDiscipline.setType(TypeCondition.DISCIPLINE);
        condDiscipline.setTargetId("chasse");
        lien.setConditions(List.of(condDiscipline));
        chapitre.setLiens(List.of(lien));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre));
        when(conditionService.estLienDisponible(lien, personnage)).thenReturn(false);

        ChapitreResponse reponse = chapitreMapper.versReponse(0, personnage);

        // Le lien est toujours present dans la reponse, juste marque indisponible.
        assertThat(reponse.liens()).hasSize(1);
        assertThat(reponse.liens().get(0).disponible()).isFalse();
    }

    @Test
    void unLienSansConditionEstToujoursDisponible() {
        Chapitre chapitre = creerChapitre(0, "texte", false);
        Chapitre cible = creerChapitre(1, "texte cible", false);

        Lien lien = new Lien();
        lien.setChapitreCible(cible);
        lien.setConditions(List.of());
        chapitre.setLiens(List.of(lien));

        when(chapitreRepository.findById(0)).thenReturn(Optional.of(chapitre));
        // Sans condition, c'est la vraie regle metier de ConditionService
        // (allMatch sur une liste vide = true) qui rendrait ce lien
        // disponible - mais conditionService est ici un mock : il faut le
        // stubber explicitement, le "true" par defaut du vrai service n'est
        // pas reproduit automatiquement.
        when(conditionService.estLienDisponible(lien, personnage)).thenReturn(true);

        ChapitreResponse reponse = chapitreMapper.versReponse(0, personnage);

        assertThat(reponse.liens().get(0).disponible()).isTrue();
    }

    @Test
    void mappeLesObjetsRamassables() {
        Chapitre chapitre = creerChapitre(20, "texte", false);
        Objet repas = new Objet();
        repas.setId("repas");
        repas.setNom("Repas");

        ObjetChap objetChap = new ObjetChap();
        objetChap.setObjet(repas);
        objetChap.setValeur(1);
        objetChap.setOptionnel(true);
        chapitre.setObjets(List.of(objetChap));

        when(chapitreRepository.findById(20)).thenReturn(Optional.of(chapitre));

        ChapitreResponse reponse = chapitreMapper.versReponse(20, personnage);

        assertThat(reponse.objets()).hasSize(1);
        assertThat(reponse.objets().get(0).objetId()).isEqualTo("repas");
        assertThat(reponse.objets().get(0).valeur()).isEqualTo(1);
        assertThat(reponse.objets().get(0).optionnel()).isTrue();
    }

    @Test
    void echoueSiLeChapitreEstIntrouvable() {
        when(chapitreRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapitreMapper.versReponse(999, personnage))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }
}