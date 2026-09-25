package com.loupsolitaire.backend.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.PorteeVol;
import com.loupsolitaire.backend.response.InventaireItemResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.InventaireService;

@ExtendWith(MockitoExtension.class)
class PersonnageMapperTest {

    @Mock
    private InventaireService inventaireService;

    @InjectMocks
    private PersonnageMapper personnageMapper;

    private Personnage personnage;

    @BeforeEach
    void setUp() {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(42);

        personnage = new Personnage();
        personnage.setId(UUID.randomUUID());
        personnage.setNom("Loup");
        personnage.setHabiliteBase(15);
        personnage.setHabilite(17);
        personnage.setHabiliteTemp(-2);
        personnage.setEnduranceMax(25);
        personnage.setEnduranceActuelle(20);
        personnage.setChapitreActuel(chapitre);
        personnage.setDisciplines(List.of(discipline(IdDiscipline.CAMOUFLAGE), discipline(IdDiscipline.GUERISON)));
    }

    private static Discipline discipline(IdDiscipline id) {
        Discipline discipline = new Discipline();
        discipline.setId(id);
        return discipline;
    }

    private static Objet objet(String id, String nom, CategorieObjet categorie) {
        Objet objet = new Objet();
        objet.setId(id);
        objet.setNom(nom);
        objet.setCategorie(categorie);
        return objet;
    }

    private static InventaireItem ligne(Objet objet, int quantite) {
        InventaireItem item = new InventaireItem();
        item.setObjet(objet);
        item.setQuantite(quantite);
        return item;
    }

    @Test
    void recopieLesCaracteristiquesDuPersonnage() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        PersonnageResponse reponse = personnageMapper.versReponse(personnage);

        assertThat(reponse.id()).isEqualTo(personnage.getId());
        assertThat(reponse.nom()).isEqualTo("Loup");
        assertThat(reponse.habiliteBase()).isEqualTo(15);
        assertThat(reponse.habilite()).isEqualTo(17);
        assertThat(reponse.habiliteTemp()).isEqualTo(-2);
        assertThat(reponse.enduranceMax()).isEqualTo(25);
        assertThat(reponse.enduranceActuelle()).isEqualTo(20);
        assertThat(reponse.disciplines()).containsExactly("CAMOUFLAGE", "GUERISON");
        assertThat(reponse.chapitreActuelId()).isEqualTo(42);
        assertThat(reponse.mort()).isFalse();
        assertThat(reponse.inventaire()).isEmpty();
    }

    @Test
    void lesChampsOptionnelsSontNullQuandIlsNeSontPasRenseignes() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        PersonnageResponse reponse = personnageMapper.versReponse(personnage);

        // Pas de Maitrise des Armes, pas de vol en attente.
        assertThat(reponse.armeMaitrisee()).isNull();
        assertThat(reponse.volEnAttente()).isNull();
    }

    @Test
    void recopieLArmeMaitriseeLeVolEnAttenteEtLaMort() {
        personnage.setArmeMaitrisee(objet("epee", "Epee", CategorieObjet.ARME));
        personnage.setVolEnAttente(PorteeVol.ARME);
        personnage.setMort(true);
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of());

        PersonnageResponse reponse = personnageMapper.versReponse(personnage);

        assertThat(reponse.armeMaitrisee()).isEqualTo("Epee");
        assertThat(reponse.volEnAttente()).isEqualTo("ARME");
        assertThat(reponse.mort()).isTrue();
    }

    @Test
    void recopieLInventaire() {
        when(inventaireService.listerInventaire(personnage)).thenReturn(List.of(
                ligne(objet("hache", "Hache", CategorieObjet.ARME), 1),
                ligne(objet("or", "Couronnes d'Or", CategorieObjet.BOURSE), 12)));

        List<InventaireItemResponse> inventaire = personnageMapper.versReponse(personnage).inventaire();

        assertThat(inventaire).containsExactly(
                new InventaireItemResponse("hache", "Hache", "ARME", 1),
                new InventaireItemResponse("or", "Couronnes d'Or", "BOURSE", 12));
    }
}