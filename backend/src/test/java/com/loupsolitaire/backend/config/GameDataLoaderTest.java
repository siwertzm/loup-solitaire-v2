package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.CatalogueEmpreinte;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Cond;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.enums.TypeCondition;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.CatalogueEmpreinteRepository;
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

    @Mock
    private CatalogueEmpreinteRepository catalogueEmpreinteRepository;

    private GameDataLoader creerLoader() {
        return new GameDataLoader(
                disciplineRepository,
                ennemiRepository,
                objetRepository,
                chapitreRepository,
                catalogueEmpreinteRepository
        );
    }

    // La base est vide par defaut et aucune empreinte n'est enregistree.
    // Les references entre fichiers (disciplines des ennemis, ennemis et
    // objets des chapitres) sont toujours trouvees, pour que chaque test
    // verifie une etape sans dependre des autres.
    @BeforeEach
    void catalogueSimule() {
        lenient().when(disciplineRepository.findById(any())).thenAnswer(inv -> {
            Discipline d = new Discipline();
            d.setId(inv.getArgument(0));
            return Optional.of(d);
        });
        lenient().when(ennemiRepository.findById(anyString())).thenAnswer(inv -> {
            Ennemi e = new Ennemi();
            e.setId(inv.getArgument(0));
            return Optional.of(e);
        });
        lenient().when(objetRepository.findById(anyString())).thenAnswer(inv -> {
            Objet o = new Objet();
            o.setId(inv.getArgument(0));
            return Optional.of(o);
        });
    }

    // =========================================================
    // Disciplines
    // =========================================================

    @Test
    void chargeLesDixDisciplinesDepuisLeFichierJson() throws Exception {

        creerLoader().run(null);

        ArgumentCaptor<List<Discipline>> captor = ArgumentCaptor.captor();
        verify(disciplineRepository).saveAll(captor.capture());

        List<Discipline> disciplines = captor.getValue();

        assertThat(disciplines).hasSize(10);

        assertThat(disciplines)
                .extracting(Discipline::getId)
                .contains(
                        IdDiscipline.CAMOUFLAGE,
                        IdDiscipline.MAITRISE_ARMES,
                        IdDiscipline.SIXIEME_SENS
                );
    }


    // =========================================================
    // Ennemis
    // =========================================================

    @Test
    void chargeLesTrenteDeuxEnnemisEtResoutLeursResistances() throws Exception {


        creerLoader().run(null);

        ArgumentCaptor<List<Ennemi>> captor = ArgumentCaptor.captor();
        verify(ennemiRepository).saveAll(captor.capture());

        List<Ennemi> ennemis = captor.getValue();

        assertThat(ennemis).hasSize(32);

        Ennemi gluatre = ennemis.stream()
                .filter(e -> e.getId().equals("gluatre"))
                .findFirst()
                .orElseThrow();

        assertThat(gluatre.getResistances())
                .extracting(Discipline::getId)
                .containsExactlyInAnyOrder(
                        IdDiscipline.PUISSANCE_PSYCHIQUE,
                        IdDiscipline.COMMUNICATION_ANIMALE
                );
    }


    @Test
    void echoueSiUneResistanceReferenceUneDisciplineInconnueEnBase() throws Exception {
        when(disciplineRepository.findById(IdDiscipline.PUISSANCE_PSYCHIQUE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> creerLoader().run(null))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    // =========================================================
    // Objets
    // =========================================================

    @Test
    void chargeLesVingtHuitObjetsAvecLeursEffets() throws Exception {

        creerLoader().run(null);

        ArgumentCaptor<List<Objet>> captor = ArgumentCaptor.captor();
        verify(objetRepository).saveAll(captor.capture());

        List<Objet> objets = captor.getValue();

        // objet.json contient desormais 28 objets (ajout de "coin",
        // utilise notamment par la condition du chapitre 53).
        assertThat(objets).hasSize(28);

        Objet casque = objets.stream()
                .filter(o -> o.getId().equals("casque"))
                .findFirst()
                .orElseThrow();

        assertThat(casque.getCategorie())
                .isEqualTo(CategorieObjet.OBJETS_SPECIAUX);

        assertThat(casque.getEffets()).hasSize(1);

        Effet effetCasque = casque.getEffets().getFirst();

        assertThat(effetCasque.getType())
                .isEqualTo(TypeEffet.ENDURANCE);

        assertThat(effetCasque.getValeur())
                .isEqualTo(2);

        Objet poignard = objets.stream()
                .filter(o -> o.getId().equals("poignard"))
                .findFirst()
                .orElseThrow();

        assertThat(poignard.getEffets()).isEmpty();
    }


    // =========================================================
    // Chapitres
    // =========================================================

    @Test
    void chargeLesTroisCentCinquanteTroisChapitresEtResoutLeursReferences() throws Exception {


        // Capture manuelle via thenAnswer plutot que verify(times(2)) +
        // ArgumentCaptor : plus robuste ici, saveAll etant une methode a
        // parametre de type propre (<S extends T> List<S> saveAll(Iterable<S>)).
        List<List<Chapitre>> appelsCaptures = new ArrayList<>();

        when(chapitreRepository.saveAll(
                org.mockito.ArgumentMatchers.<Iterable<Chapitre>>any()
        )).thenAnswer(inv -> {
            Iterable<Chapitre> arg = inv.getArgument(0);

            List<Chapitre> copie = new ArrayList<>();
            arg.forEach(copie::add);

            appelsCaptures.add(copie);

            return copie;
        });

        creerLoader().run(null);

        assertThat(appelsCaptures).hasSize(2);

        List<Chapitre> chapitresFinaux =
                appelsCaptures.getLast();

        assertThat(chapitresFinaux).hasSize(353);

        Chapitre chapitre0 = chapitresFinaux.stream()
                .filter(c -> c.getId() == 0)
                .findFirst()
                .orElseThrow();

        assertThat(chapitre0.getLiens()).hasSize(1);

        assertThat(
                chapitre0.getLiens()
                        .getFirst()
                        .getChapitreCible()
                        .getId()
        ).isEqualTo(1);

        // Chapitre 53 : deux liens dans le JSON, page "352" (fin de partie,
        // ignoree) et page "47" (conditionne a la possession de l'objet
        // "coin", conserve) -> il reste donc 1 lien reel, pas 0.
        Chapitre chapitre53 = chapitresFinaux.stream()
                .filter(c -> c.getId() == 53)
                .findFirst()
                .orElseThrow();

        assertThat(chapitre53.getLiens()).hasSize(1);

        assertThat(
                chapitre53.getLiens()
                        .getFirst()
                        .getChapitreCible()
                        .getId()
        ).isEqualTo(47);

        // Chapitre 350 : fin victorieuse du tome, seule sortie ("351")
        // egalement ignoree.
        Chapitre chapitre350 = chapitresFinaux.stream()
                .filter(c -> c.getId() == 350)
                .findFirst()
                .orElseThrow();

        assertThat(chapitre350.getLiens()).isEmpty();

        // Chapitre 17 : combat contre un Kraan.
        Chapitre chapitre17 = chapitresFinaux.stream()
                .filter(c -> c.getId() == 17)
                .findFirst()
                .orElseThrow();

        assertThat(chapitre17.isCombat()).isTrue();

        assertThat(chapitre17.getEnnemis())
                .extracting(Ennemi::getId)
                .containsExactly("kraan");

        // Chapitre 20 : objets ramassables (repas + poignard).
        Chapitre chapitre20 = chapitresFinaux.stream()
                .filter(c -> c.getId() == 20)
                .findFirst()
                .orElseThrow();

        assertThat(chapitre20.getObjets()).hasSize(2);
    }


    // =========================================================
    // DATA-00 : empreinte des JSON
    // =========================================================

    private static CatalogueEmpreinte empreinte(String valeur) {
        CatalogueEmpreinte ligne = new CatalogueEmpreinte();
        ligne.setEmpreinte(valeur);
        return ligne;
    }

    @Test
    void neRechargeRienSiLEmpreinteDesJsonNAPasChange() throws Exception {
        when(catalogueEmpreinteRepository.findById(CatalogueEmpreinte.ID_UNIQUE))
                .thenReturn(Optional.of(empreinte(GameDataLoader.calculerEmpreinte())));

        creerLoader().run(null);

        verify(disciplineRepository, never()).findAll();
        verify(chapitreRepository, never()).saveAll(any());
        verify(catalogueEmpreinteRepository, never()).save(any());
    }

    @Test
    void rechargeToutEtEnregistreLaNouvelleEmpreinteSiElleAChange() throws Exception {
        CatalogueEmpreinte ancienne = empreinte("ancienne");
        when(catalogueEmpreinteRepository.findById(CatalogueEmpreinte.ID_UNIQUE))
                .thenReturn(Optional.of(ancienne));

        creerLoader().run(null);

        verify(disciplineRepository).saveAll(any());
        verify(chapitreRepository, times(2)).saveAll(any());
        verify(catalogueEmpreinteRepository).save(ancienne);
        assertThat(ancienne.getEmpreinte())
                .isEqualTo(GameDataLoader.calculerEmpreinte())
                .hasSize(64);
        assertThat(ancienne.getChargeLe()).isNotNull();
    }

    @Test
    void metAJourUnObjetDejaEnBaseSansLeRecreer() throws Exception {
        // Objet en base avec des donnees perimees : meme instance mise a
        // jour (Hibernate suit l'instance chargee), effets remplaces.
        Objet casqueEnBase = new Objet();
        casqueEnBase.setId("casque");
        casqueEnBase.setNom("Ancien nom");
        Effet perime = new Effet();
        perime.setType(TypeEffet.HABILITE);
        perime.setValeur(99);
        casqueEnBase.getEffets().add(perime);
        List<Effet> collectionSuivieParHibernate = casqueEnBase.getEffets();
        when(objetRepository.findAll()).thenReturn(List.of(casqueEnBase));

        creerLoader().run(null);

        ArgumentCaptor<List<Objet>> captor = ArgumentCaptor.captor();
        verify(objetRepository).saveAll(captor.capture());
        Objet casque = captor.getValue().stream()
                .filter(o -> o.getId().equals("casque"))
                .findFirst()
                .orElseThrow();

        assertThat(casque).isSameAs(casqueEnBase);
        assertThat(casque.getNom()).isNotEqualTo("Ancien nom");
        assertThat(casque.getEffets()).isSameAs(collectionSuivieParHibernate);
        assertThat(casque.getEffets()).singleElement()
                .satisfies(effet -> {
                    assertThat(effet.getType()).isEqualTo(TypeEffet.ENDURANCE);
                    assertThat(effet.getValeur()).isEqualTo(2);
                });
        // Les anciens effets sont supprimes en base avant les nouveaux.
        verify(objetRepository).flush();
    }

    // =========================================================
    // OPS-03 : controle d'integrite
    // =========================================================

    private static Chapitre chapitre(int id, int nombreDeLiens) {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(id);
        for (int i = 0; i < nombreDeLiens; i++) {
            chapitre.getLiens().add(new com.loupsolitaire.backend.model.Lien());
        }
        return chapitre;
    }

    @Test
    void laFinDuTomeEstLeSeulChapitreAutoriseSansLien() {
        List<Chapitre> chapitres = List.of(chapitre(1, 2), chapitre(GameDataLoader.CHAPITRE_FIN_DU_TOME, 0));

        assertThatCode(() -> GameDataLoader.verifierIntegrite(chapitres)).doesNotThrowAnyException();
    }

    @Test
    void refuseUnChapitreSansLien() {
        List<Chapitre> chapitres = List.of(chapitre(1, 2), chapitre(42, 0), chapitre(7, 0));

        assertThatThrownBy(() -> GameDataLoader.verifierIntegrite(chapitres))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[7, 42]");
    }

    // =========================================================
    // Validation des conditions au chargement
    // (le chargement complet ci-dessus prouve deja que les 137
    // conditions du livre sont valides)
    // =========================================================

    private static final Predicate<String> OBJETS_CONNUS = Set.of("torche", "or", "marteau")::contains;

    private static Cond cond(TypeCondition type, String targetId, String valeur) {
        Cond cond = new Cond();
        cond.setType(type);
        cond.setTargetId(targetId);
        cond.setValeur(valeur);
        return cond;
    }

    @Test
    void accepteLesConditionsBienFormees() {
        GameDataLoader.validerCondition(cond(TypeCondition.HASARD, null, "[0, 4]"), OBJETS_CONNUS);
        // REGLE-11 : marqueur sans cible ni valeur (combat a mains nues).
        GameDataLoader.validerCondition(cond(TypeCondition.SANS_ARME, null, null), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.DISCIPLINE, "bouclier_psychique", "-1"), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.OBJET, "torche", "-1"), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.BOURSE, "or", "10"), OBJETS_CONNUS);
        // ARME sans cible : "une arme quelconque".
        GameDataLoader.validerCondition(cond(TypeCondition.ARME, null, "1"), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.ARME, "marteau", "1"), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.FUITE, null, "3"), OBJETS_CONNUS);
        // Valeur non utilisee : peut manquer.
        GameDataLoader.validerCondition(cond(TypeCondition.VICTOIRE, null, null), OBJETS_CONNUS);
        GameDataLoader.validerCondition(cond(TypeCondition.PERMANENT, null, null), OBJETS_CONNUS);
    }

    @Test
    void refuseUneValeurNumeriqueMalEcrite() {
        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.ENDURANCE, null, "10 points"), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("10 points");
    }

    @Test
    void refuseUneValeurManquanteQuandElleEstUtilisee() {
        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.FUITE, null, null), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseUnePlageDeHasardInvalide() {
        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.HASARD, null, "[0, 10]"), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseUneDisciplineInconnue() {
        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.DISCIPLINE, "telepathie", "1"), OBJETS_CONNUS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telepathie");
    }

    @Test
    void refuseUnObjetInconnuOuManquant() {
        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.OBJET, "torch", "1"), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("torch");

        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.BOURSE, null, "10"), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> GameDataLoader.validerCondition(
                cond(TypeCondition.ARME, "baton_magique", "1"), OBJETS_CONNUS))
                .isInstanceOf(IllegalStateException.class);
    }
}