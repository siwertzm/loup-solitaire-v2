package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.CombatEnnemi;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Ennemi;
import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.ObjetChapitreRamasse;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.TirageCreation;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.InventaireItemRepository;
import com.loupsolitaire.backend.repository.ObjetChapitreRamasseRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.TirageCreationRepository;
import com.loupsolitaire.backend.response.ExportDonneesResponse;
import com.loupsolitaire.backend.response.ExportDonneesResponse.PersonnageExporte;

import tools.jackson.databind.json.JsonMapper;

// RGPD-02 : l'export contient toutes les donnees du compte, sans secret.
@ExtendWith(MockitoExtension.class)
class ExportDonneesServiceTest {

    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private InventaireItemRepository inventaireItemRepository;
    @Mock
    private ObjetChapitreRamasseRepository objetChapitreRamasseRepository;
    @Mock
    private CombatRepository combatRepository;
    @Mock
    private TirageCreationRepository tirageCreationRepository;

    @InjectMocks
    private ExportDonneesService exportDonneesService;

    @Test
    void exporteLeCompteLesPersonnagesEtLeurHistoriqueSansSecret() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setPassword("hash-secret");
        utilisateur.setEmailVerifie(true);

        Objet epee = objet("Épée");
        Chapitre chapitre12 = chapitre(12);
        Personnage loup = new Personnage();
        loup.setId(UUID.randomUUID());
        loup.setNom("Loup");
        loup.setEnduranceActuelle(20);
        loup.setChapitreActuel(chapitre12);
        loup.setArmeMaitrisee(epee);
        Discipline guerison = new Discipline();
        guerison.setId(IdDiscipline.GUERISON);
        loup.setDisciplines(List.of(guerison));
        loup.getChapitresParcourus().addAll(List.of(1, 12));

        InventaireItem item = new InventaireItem();
        item.setObjet(epee);
        item.setQuantite(1);
        ObjetChapitreRamasse ramasse = new ObjetChapitreRamasse();
        ramasse.setChapitre(chapitre12);
        ramasse.setObjet(epee);
        ramasse.setQuantite(1);
        Ennemi kraan = new Ennemi();
        kraan.setNom("Kraan");
        CombatEnnemi combatEnnemi = new CombatEnnemi();
        combatEnnemi.setEnnemi(kraan);
        Combat combat = new Combat();
        combat.setChapitreId(12);
        combat.setStatut(StatutCombat.VICTOIRE);
        combat.setAssautsLivres(3);
        combat.setEnnemis(List.of(combatEnnemi));
        TirageCreation tirage = new TirageCreation();
        tirage.setHasardHabilite(4);
        tirage.setHasardEndurance(7);
        tirage.setCreeLe(Instant.now());

        when(personnageRepository.findByUtilisateur(utilisateur)).thenReturn(List.of(loup));
        when(inventaireItemRepository.findByPersonnageId(loup.getId())).thenReturn(List.of(item));
        when(objetChapitreRamasseRepository.findByPersonnageId(loup.getId())).thenReturn(List.of(ramasse));
        when(combatRepository.findByPersonnage(loup)).thenReturn(List.of(combat));
        when(tirageCreationRepository.findById(utilisateur.getId())).thenReturn(Optional.of(tirage));

        ExportDonneesResponse export = exportDonneesService.construire(utilisateur);

        assertThat(export.compte().nomUtilisateur()).isEqualTo("marius");
        assertThat(export.compte().email()).isEqualTo("marius@example.com");
        assertThat(export.tirageCreationEnCours().hasardEndurance()).isEqualTo(7);
        PersonnageExporte p = export.personnages().getFirst();
        assertThat(p.nom()).isEqualTo("Loup");
        assertThat(p.disciplines()).containsExactly("GUERISON");
        assertThat(p.armeMaitrisee()).isEqualTo("Épée");
        assertThat(p.chapitreActuel()).isEqualTo(12);
        assertThat(p.chapitresParcourus()).containsExactly(1, 12);
        assertThat(p.inventaire()).containsExactly(new ExportDonneesResponse.ObjetPossede("Épée", 1));
        assertThat(p.objetsRamasses()).containsExactly(new ExportDonneesResponse.ObjetRamasse(12, "Épée", 1));
        assertThat(p.combats().getFirst().ennemis()).containsExactly("Kraan");
        assertThat(p.combats().getFirst().statut()).isEqualTo("VICTOIRE");

        String json = JsonMapper.builder().build().writeValueAsString(export);
        assertThat(json).doesNotContain("hash-secret").doesNotContainIgnoringCase("password");
    }

    private static Objet objet(String nom) {
        Objet objet = new Objet();
        objet.setNom(nom);
        return objet;
    }

    private static Chapitre chapitre(int id) {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(id);
        return chapitre;
    }
}
