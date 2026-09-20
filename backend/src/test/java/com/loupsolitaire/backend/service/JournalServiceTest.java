package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Effet;
import com.loupsolitaire.backend.model.ObjetChap;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.response.ChapitreParcouruResponse;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    private static final String POINTS = "\u2026";

    @Mock
    private ChapitreRepository chapitreRepository;

    @InjectMocks
    private JournalService service;

    private Chapitre chapitre(int id, String text) {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(id);
        chapitre.setText(text);
        return chapitre;
    }

    // Reponse attendue pour un chapitre sans combat, sans effet, sans objet.
    private ChapitreParcouruResponse simple(int id, String extrait) {
        return new ChapitreParcouruResponse(id, extrait, false, false, false, false);
    }

    // Parcours dans l'ordre chronologique, comme stocke dans Personnage.
    private Personnage personnageAvecParcours(Integer... ids) {
        Personnage personnage = new Personnage();
        personnage.setChapitresParcourus(new ArrayList<>(List.of(ids)));
        return personnage;
    }

    // =========================================================
    // lister
    // =========================================================

    @Test
    void listerRenvoieDuPlusRecentAuPlusAncienAvecLExtraitDeChaqueChapitre() {
        Personnage personnage = personnageAvecParcours(0, 1, 85);
        when(chapitreRepository.findAllById(Set.of(0, 1, 85))).thenReturn(List.of(
                chapitre(0, "Au nord du royaume"),
                chapitre(1, "Il faut vous hater"),
                chapitre(85, "Le chemin est large")));

        List<ChapitreParcouruResponse> journal = service.lister(personnage);

        assertThat(journal).containsExactly(
                simple(85, "Le chemin est large"),
                simple(1, "Il faut vous hater"),
                simple(0, "Au nord du royaume"));
    }

    @Test
    void listerGardeChaqueArriveeMemeSurUnChapitreRevisiteMaisNeChargeLeTexteQuUneFois() {
        // Retour paye apres une mort narrative : 85 est traverse deux fois.
        Personnage personnage = personnageAvecParcours(0, 85, 322, 85);
        when(chapitreRepository.findAllById(Set.of(0, 85, 322))).thenReturn(List.of(
                chapitre(0, "Intro"),
                chapitre(85, "Le chemin"),
                chapitre(322, "Le carrefour")));

        List<ChapitreParcouruResponse> journal = service.lister(personnage);

        assertThat(journal).extracting(ChapitreParcouruResponse::chapitreId)
                .containsExactly(85, 322, 85, 0);
        assertThat(journal).extracting(ChapitreParcouruResponse::extrait)
                .containsExactly("Le chemin", "Le carrefour", "Le chemin", "Intro");
        verify(chapitreRepository, times(1)).findAllById(Set.of(0, 85, 322));
    }

    @Test
    void listerRenvoieUneListeVideSansInterrogerLaBaseSiAucunChapitre() {
        List<ChapitreParcouruResponse> journal = service.lister(new Personnage());

        assertThat(journal).isEmpty();
        verifyNoInteractions(chapitreRepository);
    }

    @Test
    void listerDonneUnExtraitVideSiLeChapitreEstIntrouvable() {
        Personnage personnage = personnageAvecParcours(5);
        when(chapitreRepository.findAllById(Set.of(5))).thenReturn(List.of());

        assertThat(service.lister(personnage)).containsExactly(simple(5, ""));
    }

    @Test
    void listerNettoieLeTexteDeChaqueChapitre() {
        Personnage personnage = personnageAvecParcours(28);
        when(chapitreRepository.findAllById(Set.of(28))).thenReturn(List.of(
                chapitre(28, "Rendez-vous au <strong>130</strong>.\n\nSinon au <strong>147</strong>.")));

        assertThat(service.lister(personnage))
                .containsExactly(simple(28, "Rendez-vous au 130. Sinon au 147."));
    }

    @Test
    void listerIndiqueSiChaqueChapitreAUnCombatDesEffetsOuDesObjets() {
        Chapitre combat = chapitre(1, "c");
        combat.setCombat(true);
        Chapitre effets = chapitre(2, "e");
        effets.getEffets().add(new Effet());
        Chapitre objets = chapitre(3, "o");
        objets.getObjets().add(new ObjetChap());
        Chapitre tout = chapitre(4, "t");
        tout.setCombat(true);
        tout.getEffets().add(new Effet());
        tout.getObjets().add(new ObjetChap());
        Chapitre rien = chapitre(5, "r");

        Personnage personnage = personnageAvecParcours(1, 2, 3, 4, 5);
        when(chapitreRepository.findAllById(Set.of(1, 2, 3, 4, 5)))
                .thenReturn(List.of(combat, effets, objets, tout, rien));

        // Du plus recent au plus ancien : 5, 4, 3, 2, 1.
        assertThat(service.lister(personnage)).containsExactly(
                new ChapitreParcouruResponse(5, "r", false, false, false, false),
                new ChapitreParcouruResponse(4, "t", true, true, true, false),
                new ChapitreParcouruResponse(3, "o", false, false, true, false),
                new ChapitreParcouruResponse(2, "e", false, true, false, false),
                new ChapitreParcouruResponse(1, "c", true, false, false, false));
    }

    @Test
    void listerRepeteLesIndicateursSurUnChapitreRevisite() {
        Chapitre combat = chapitre(85, "Le chemin");
        combat.setCombat(true);
        Personnage personnage = personnageAvecParcours(85, 1, 85);
        when(chapitreRepository.findAllById(Set.of(85, 1)))
                .thenReturn(List.of(combat, chapitre(1, "Debut")));

        assertThat(service.lister(personnage)).containsExactly(
                new ChapitreParcouruResponse(85, "Le chemin", true, false, false, false),
                simple(1, "Debut"),
                new ChapitreParcouruResponse(85, "Le chemin", true, false, false, false));
    }

        @Test
    void listerIndiqueLesEtapesOuLePersonnageEstMort() {
        // Parcours : 0, 47, 53 (mort), 47 (retour paye), 322.
        Personnage personnage = personnageAvecParcours(0, 47, 53, 47, 322);
        personnage.getEtapesMortelles().add(2);
        when(chapitreRepository.findAllById(Set.of(0, 47, 53, 322))).thenReturn(List.of(
                chapitre(0, "a"), chapitre(47, "b"), chapitre(53, "c"), chapitre(322, "d")));

        List<ChapitreParcouruResponse> journal = service.lister(personnage);

        // Du plus recent au plus ancien : 322, 47, 53, 47, 0.
        assertThat(journal).extracting(ChapitreParcouruResponse::chapitreId)
                .containsExactly(322, 47, 53, 47, 0);
        assertThat(journal).extracting(ChapitreParcouruResponse::mort)
                .containsExactly(false, false, true, false, false);
    }

    @Test
    void listerNeMarqueMortQueLArriveeOuLePersonnageEstMortSurUnChapitreRevisite() {
        // 85 est traverse deux fois ; on ne meurt que la seconde (position 3).
        Personnage personnage = personnageAvecParcours(0, 85, 322, 85);
        personnage.getEtapesMortelles().add(3);
        when(chapitreRepository.findAllById(Set.of(0, 85, 322))).thenReturn(List.of(
                chapitre(0, "a"), chapitre(85, "b"), chapitre(322, "c")));

        List<ChapitreParcouruResponse> journal = service.lister(personnage);

        // Du plus recent au plus ancien : 85 (mort), 322, 85 (indemne), 0.
        assertThat(journal).extracting(ChapitreParcouruResponse::mort)
                .containsExactly(true, false, false, false);
    }

    // =========================================================
    // extraire
    // =========================================================

    @Test
    void extraireRetireLesBalisesEnGardantLeurContenu() {
        assertThat(JournalService.extraire("Un <strong>Repas</strong>. Apres, vous partez."))
                .isEqualTo("Un Repas. Apres, vous partez.");
    }

    @Test
    void extraireRetireLeJetonEnnemi() {
        assertThat(JournalService.extraire("Combattez ce Kraan.\n\n[[ENNEMI]]\n\nSi vous le tuez, partez."))
                .isEqualTo("Combattez ce Kraan. Si vous le tuez, partez.");
    }

    @Test
    void extraireRemplaceLesRetoursALaLigneEtLesEspacesMultiplesParUnSeulEspace() {
        assertThat(JournalService.extraire("Un.\n\nDeux.\nTrois.   Quatre."))
                .isEqualTo("Un. Deux. Trois. Quatre.");
    }

    @Test
    void extraireLaisseUnTexteCourtIntactSansPointsDeSuspension() {
        assertThat(JournalService.extraire("Un texte court.")).isEqualTo("Un texte court.");
    }

    @Test
    void extraireNeCoupePasUnTexteDePileLaLongueurMaximale() {
        String texte = "a".repeat(JournalService.LONGUEUR_MAX_EXTRAIT);

        assertThat(JournalService.extraire(texte)).isEqualTo(texte);
    }

    @Test
    void extraireCoupeSurUnMotEtAjouteLesPointsDeSuspension() {
        // 20 mots de 9 lettres separes par un espace : la limite (120) tombe au
        // milieu du 13e mot, on garde donc les 12 premiers mots entiers.
        String texte = String.join(" ", Collections.nCopies(20, "abcdefghi"));

        String extrait = JournalService.extraire(texte);

        assertThat(extrait).isEqualTo(String.join(" ", Collections.nCopies(12, "abcdefghi")) + POINTS);
        assertThat(extrait.length()).isLessThanOrEqualTo(JournalService.LONGUEUR_MAX_EXTRAIT + 1);
    }

    @Test
    void extraireFaitUneCoupeFrancheSiLeDebutNeContientAucunEspaceExploitable() {
        String extrait = JournalService.extraire("a".repeat(200));

        assertThat(extrait).isEqualTo("a".repeat(JournalService.LONGUEUR_MAX_EXTRAIT) + POINTS);
    }

    @Test
    void extraireNeCompteQueLeTexteVisibleNonLeBalisage() {
        // Beaucoup de balises : le texte visible (120 lettres) tient sans coupe.
        String texte = "<strong>" + "a".repeat(JournalService.LONGUEUR_MAX_EXTRAIT) + "</strong>";

        assertThat(JournalService.extraire(texte)).isEqualTo("a".repeat(JournalService.LONGUEUR_MAX_EXTRAIT));
    }

    @Test
    void extraireRenvoieUneChaineVideSiLeTexteEstNulVideOuSeulementDuBalisage() {
        assertThat(JournalService.extraire(null)).isEmpty();
        assertThat(JournalService.extraire("")).isEmpty();
        assertThat(JournalService.extraire("[[ENNEMI]]")).isEmpty();
        assertThat(JournalService.extraire("<strong></strong>")).isEmpty();
    }
}