package com.loupsolitaire.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.response.EffetObjetResponse;
import com.loupsolitaire.backend.response.ObjetResponse;
import com.loupsolitaire.backend.util.Textes;

@ExtendWith(MockitoExtension.class)
class ObjetControllerTest {

    @Mock
    private ObjetRepository objetRepository;

    @InjectMocks
    private ObjetController objetController;

    private static Objet objet(String id, String nom, CategorieObjet categorie) {
        Objet objet = new Objet();
        objet.setId(id);
        objet.setNom(nom);
        objet.setDescription("Description de " + nom);
        objet.setCategorie(categorie);
        return objet;
    }

    @Test
    void trieLesObjetsParNomSansTenirCompteDesMajuscules() {
        when(objetRepository.findAll()).thenReturn(List.of(
                objet("repas", "repas", CategorieObjet.REPAS),
                objet("epee", "Epee", CategorieObjet.ARME),
                objet("baton", "Baton", CategorieObjet.ARME)));

        List<ObjetResponse> objets = objetController.lister();

        // "repas" en minuscule reste bien apres "Epee" (tri insensible a la casse).
        assertThat(objets).extracting(ObjetResponse::id).containsExactly("baton", "epee", "repas");
    }

    @Test
    void recopieLaCategorieEtLesEffetsDeChaqueObjet() {
        Objet potion = objet("potion_de_soin", "Potion de Laumspur", CategorieObjet.OBJET);
        Effet soin = new Effet();
        soin.setType(TypeEffet.ENDURANCE);
        soin.setValeur(4);
        potion.setEffets(List.of(soin));
        when(objetRepository.findAll()).thenReturn(List.of(potion));

        ObjetResponse reponse = objetController.lister().getFirst();

        assertThat(reponse.nom()).isEqualTo("Potion de Laumspur");
        assertThat(reponse.description()).isEqualTo("Description de Potion de Laumspur");
        assertThat(reponse.categorie()).isEqualTo(CategorieObjet.OBJET);
        assertThat(reponse.effets()).containsExactly(new EffetObjetResponse("ENDURANCE", 4));
    }

    @Test
    void enAnglaisRenvoieEtTrieLesNomsDeLaVo() {
        Objet poignard = objet("poignard", "Poignard", CategorieObjet.ARME);
        poignard.setNomEn("Dagger");
        poignard.setDescriptionEn("");
        Objet baton = objet("baton", "Baton", CategorieObjet.ARME);
        baton.setNomEn("Quarterstaff");
        baton.setDescriptionEn("");
        when(objetRepository.findAll()).thenReturn(List.of(baton, poignard));
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader(Textes.ENTETE_LANGUE, "en");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
        try {
            List<ObjetResponse> objets = objetController.lister();

            // En francais : Baton, Poignard ; en anglais : Dagger, Quarterstaff.
            assertThat(objets).extracting(ObjetResponse::nom).containsExactly("Dagger", "Quarterstaff");
            assertThat(objets.getFirst().description()).isEmpty();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
