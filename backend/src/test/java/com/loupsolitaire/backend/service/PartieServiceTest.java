package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.TirageCreation;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.TirageResponse;
import com.loupsolitaire.backend.service.mapper.ChapitreMapper;
import com.loupsolitaire.backend.service.mapper.CombatMapper;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;
import com.loupsolitaire.backend.service.record.TourJoue;

@ExtendWith(MockitoExtension.class)
class PartieServiceTest {

    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ObjetRepository objetRepository;
    @Mock
    private PersonnageService personnageService;
    @Mock
    private InventaireService inventaireService;
    @Mock
    private ObjetService objetService;
    @Mock
    private EffetChapitreService effetChapitreService;
    @Mock
    private CombatService combatService;
    @Mock
    private JournalService journalService;
    @Mock
    private TirageCreationService tirageCreationService;
    @Mock
    private PersonnageMapper personnageMapper;
    @Mock
    private ChapitreMapper chapitreMapper;
    @Mock
    private CombatMapper combatMapper;

    @InjectMocks
    private PartieService partieService;

    private final UUID personnageId = UUID.randomUUID();
    private final UtilisateurConnecte marius = new UtilisateurConnecte(UUID.randomUUID());
    private final UtilisateurConnecte intrus = new UtilisateurConnecte(UUID.randomUUID());

    private Personnage personnage;
    private PersonnageResponse reponse;

    @BeforeEach
    void setUp() {
        personnage = personnageDe(marius, null);
        reponse = new PersonnageResponse(personnageId, "Loup Solitaire", 15, 15, 0, 20, 20,
                List.of(), null, 17, null, false, List.of());
    }

    private Personnage personnageDe(UtilisateurConnecte proprietaire, Instant derniereActivite) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(proprietaire.id());

        Chapitre chapitre = new Chapitre();
        chapitre.setId(17);

        Personnage p = new Personnage();
        p.setId(UUID.randomUUID());
        p.setUtilisateur(utilisateur);
        p.setChapitreActuel(chapitre);
        p.setDerniereActivite(derniereActivite);
        return p;
    }

    private Objet objet(String id) {
        Objet objet = new Objet();
        objet.setId(id);
        when(objetRepository.findById(id)).thenReturn(Optional.of(objet));
        return objet;
    }

    // =========================================================
    // Chargement : proprietaire, 404, 403, lecture vs modification
    // =========================================================

    @Test
    void uneLectureChargeLePersonnageSansVerrou() {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        assertThat(partieService.recupererPersonnage(personnageId, marius)).isEqualTo(reponse);

        verify(personnageRepository, never()).findByIdPourModification(any());
    }

    @Test
    void uneActionChargeLePersonnageAvecLeVerrouOptimiste() {
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        partieService.avancerVersChapitre(personnageId, 85, marius);

        verify(personnageService).avancerVersChapitre(personnage, 85);
        verify(personnageRepository, never()).findById(any());
    }

    @Test
    void renvoie404SiLePersonnageEstIntrouvable() {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partieService.recupererPersonnage(personnageId, marius))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    @Test
    void refuseLaLectureDuPersonnageDUnAutreJoueur() {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        assertThatThrownBy(() -> partieService.chapitreCourant(personnageId, intrus))
                .isInstanceOf(AccesRefuseException.class);

        verifyNoInteractions(chapitreMapper);
    }

    @Test
    void refuseToutesActionsSurLePersonnageDUnAutreJoueur() {
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));

        assertThatThrownBy(() -> partieService.ressusciter(personnageId, intrus))
                .isInstanceOf(AccesRefuseException.class);

        verifyNoInteractions(personnageService);
    }

    @Test
    void renvoie404SiL_objetEstIntrouvableSansRienModifier() {
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partieService.ramasserObjet(personnageId, "inconnu", marius))
                .isInstanceOf(RessourceNonTrouveeException.class);

        verifyNoInteractions(personnageService);
    }

    // =========================================================
    // Personnages
    // =========================================================

    private TirageCreation tirage(int hasardHabilite, int hasardEndurance) {
        TirageCreation tirage = new TirageCreation();
        tirage.setUtilisateurId(marius.id());
        tirage.setHasardHabilite(hasardHabilite);
        tirage.setHasardEndurance(hasardEndurance);
        return tirage;
    }

    @Test
    void tirerCaracteristiquesRenvoieLeTirageDuServeurEtLesTotaux() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(marius.id());
        when(utilisateurRepository.findByIdPourModification(marius.id())).thenReturn(Optional.of(utilisateur));
        when(tirageCreationService.tirerOuRelire(marius.id())).thenReturn(tirage(4, 7));

        assertThat(partieService.tirerCaracteristiques(marius)).isEqualTo(new TirageResponse(4, 7, 14, 27));
    }

    @Test
    void creerPersonnageUtiliseLeTirageEnAttenteEtRattacheLePersonnageAL_utilisateurConnecte() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(marius.id());
        List<IdDiscipline> disciplines = List.of(IdDiscipline.CHASSE);
        when(utilisateurRepository.findByIdPourModification(marius.id())).thenReturn(Optional.of(utilisateur));
        when(tirageCreationService.consommer(marius.id())).thenReturn(tirage(5, 3));
        when(personnageService.creerPersonnage(utilisateur, "Loup", disciplines, 5, 3)).thenReturn(personnage);
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        assertThat(partieService.creerPersonnage(marius, "Loup", disciplines)).isEqualTo(reponse);
    }

    @Test
    void creerPersonnageRenvoie400SansTirageEnAttente() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(marius.id());
        when(utilisateurRepository.findByIdPourModification(marius.id())).thenReturn(Optional.of(utilisateur));
        when(tirageCreationService.consommer(marius.id()))
                .thenThrow(new IllegalArgumentException("Aucun tirage en attente"));

        assertThatThrownBy(() -> partieService.creerPersonnage(marius, "Loup", List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(personnageService);
    }

    @Test
    void creerPersonnageRenvoie404SiLeCompteN_existePlus() {
        when(utilisateurRepository.findByIdPourModification(marius.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partieService.creerPersonnage(marius, "Loup", List.of()))
                .isInstanceOf(RessourceNonTrouveeException.class);

        verifyNoInteractions(personnageService, tirageCreationService);
    }

    @Test
    void listerPersonnagesMetLePlusRecemmentJoueEnTete() {
        Utilisateur utilisateur = new Utilisateur();
        Personnage ancien = personnageDe(marius, Instant.parse("2026-09-01T10:00:00Z"));
        Personnage recent = personnageDe(marius, Instant.parse("2026-09-20T10:00:00Z"));
        Personnage jamaisJoue = personnageDe(marius, null);
        PersonnageResponse reponseAncien = reponse;
        PersonnageResponse reponseRecent = new PersonnageResponse(recent.getId(), "Recent", 15, 15, 0, 20, 20,
                List.of(), null, 17, null, false, List.of());
        PersonnageResponse reponseJamais = new PersonnageResponse(jamaisJoue.getId(), "Jamais", 15, 15, 0, 20, 20,
                List.of(), null, 17, null, false, List.of());

        when(utilisateurRepository.findById(marius.id())).thenReturn(Optional.of(utilisateur));
        when(personnageRepository.findByUtilisateur(utilisateur)).thenReturn(List.of(jamaisJoue, ancien, recent));
        when(personnageMapper.versReponse(ancien)).thenReturn(reponseAncien);
        when(personnageMapper.versReponse(recent)).thenReturn(reponseRecent);
        when(personnageMapper.versReponse(jamaisJoue)).thenReturn(reponseJamais);

        assertThat(partieService.listerPersonnages(marius))
                .containsExactly(reponseRecent, reponseAncien, reponseJamais);
    }

    @Test
    void consommerObjetAppliqueL_effetAvantDeRetirerL_objet() {
        Objet potion = objet("potion_de_soin");
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        partieService.consommerObjet(personnageId, "potion_de_soin", marius);

        InOrder ordre = inOrder(objetService, inventaireService);
        ordre.verify(objetService).appliquerEffetsConsommation(personnage, potion);
        ordre.verify(inventaireService).retirerObjet(personnage, potion, 1);
    }

    @Test
    void consommerObjetRefuseUneFoisLeCombatEngage() {
        // REGLE-05 : en combat, un objet remplace l'attaque (action OBJET).
        Combat combat = new Combat();
        combat.setStatut(StatutCombat.EN_COURS);
        combat.setAssautsLivres(1);
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.of(combat));

        assertThatThrownBy(() -> partieService.consommerObjet(personnageId, "potion_de_soin", marius))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OBJET");

        verifyNoInteractions(objetService, inventaireService);
    }

    @Test
    void consommerObjetRefuseDesQueLeCombatEstLanceMemeAvantLePremierAssaut() {
        // Combat lance (page de combat ouverte), aucun assaut joue : l'objet
        // doit passer par l'action OBJET, qui coute un tour.
        Combat combat = new Combat();
        combat.setStatut(StatutCombat.EN_COURS);
        combat.setAssautsLivres(0);
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.of(combat));

        assertThatThrownBy(() -> partieService.consommerObjet(personnageId, "alether", marius))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OBJET");

        verifyNoInteractions(objetService, inventaireService);
    }

    @Test
    void consommerObjetAutoriseUneFoisLeCombatTermine() {
        // Potion bue apres la victoire, sur le meme chapitre.
        Objet potion = objet("potion_de_soin");
        Combat combat = new Combat();
        combat.setStatut(StatutCombat.VICTOIRE);
        combat.setAssautsLivres(3);
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.of(combat));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        partieService.consommerObjet(personnageId, "potion_de_soin", marius);

        verify(objetService).appliquerEffetsConsommation(personnage, potion);
        verify(inventaireService).retirerObjet(personnage, potion, 1);
    }

    @Test
    void echangerObjetTransmetLesObjetsDansLeBonOrdre() {
        Objet marteau = objet("marteau");
        Objet baton = objet("baton");
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        partieService.echangerObjet(personnageId, "marteau", "baton", marius);

        // Signature du service metier : (personnage, objetARetirer, objetAAjouter).
        verify(personnageService).echangerObjet(personnage, baton, marteau);
    }

    @Test
    void retirerObjetTransmetLaQuantite() {
        Objet repas = objet("repas");
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponse);

        partieService.retirerObjet(personnageId, "repas", 3, marius);

        verify(inventaireService).retirerObjet(personnage, repas, 3);
    }

    @Test
    void supprimerPersonnageSeContenteDUneLectureSimple() {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        partieService.supprimerPersonnage(personnageId, marius);

        verify(personnageService).supprimerPersonnage(personnage);
        verify(personnageRepository, never()).findByIdPourModification(any());
    }

    // =========================================================
    // Combat
    // =========================================================

    @Test
    void jouerTourSansObjetNeChercheAucunObjet() {
        Combat combat = new Combat();
        CombatResponse reponseCombat = new CombatResponse(UUID.randomUUID(), 17, List.of(), 1, false, 0,
                "EN_COURS", false, null);
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null)).thenReturn(new TourJoue(combat, null));
        when(combatMapper.versReponse(combat, null)).thenReturn(reponseCombat);

        assertThat(partieService.jouerTour(personnageId, ActionCombat.ATTAQUE, null, marius)).isEqualTo(reponseCombat);

        verifyNoInteractions(objetRepository);
    }

    @Test
    void jouerTourAvecObjetLeChargeAvantDeJouer() {
        Objet potion = objet("potion_de_soin");
        Combat combat = new Combat();
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.jouerTour(personnage, ActionCombat.OBJET, potion)).thenReturn(new TourJoue(combat, null));

        partieService.jouerTour(personnageId, ActionCombat.OBJET, "potion_de_soin", marius);

        verify(combatService).jouerTour(personnage, ActionCombat.OBJET, potion);
    }

    @Test
    void combatActuelRenvoie404SiAucunCombatN_aEteInitie() {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partieService.combatActuel(personnageId, marius))
                .isInstanceOf(RessourceNonTrouveeException.class)
                .hasMessageContaining("17");
    }

    @Test
    void initierCombatVerrouilleLePersonnage() {
        Combat combat = new Combat();
        when(personnageRepository.findByIdPourModification(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.initierCombat(personnage)).thenReturn(combat);

        partieService.initierCombat(personnageId, marius);

        verify(combatMapper).versReponse(combat);
    }
}