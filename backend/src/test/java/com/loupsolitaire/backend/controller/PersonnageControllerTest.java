package com.loupsolitaire.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.json.JsonMapper;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.AccesRefuseException;
import com.loupsolitaire.backend.exception.GlobalExceptionHandler;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.request.CreerPersonnageRequest;
import com.loupsolitaire.backend.response.ChapitreParcouruResponse;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.PartieService;

/**
 * Couche HTTP uniquement : routes, codes de retour, traduction des erreurs
 * et transmission des bons parametres a PartieService. La logique
 * (proprietaire, regles du jeu) est testee dans PartieServiceTest et les
 * tests des services metier.
 */
@ExtendWith(MockitoExtension.class)
class PersonnageControllerTest {

    @Mock
    private PartieService partieService;

    @InjectMocks
    private PersonnageController controller;

    private MockMvc mockMvc;
    // Jackson 3 gere java.time nativement : plus besoin de findAndRegisterModules().
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final UUID personnageId = UUID.randomUUID();
    private final UtilisateurConnecte marius = new UtilisateurConnecte(idDe("marius"));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
        authentifierComme(marius);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Identifiant fixe derive du nom (voir UtilisateurConnecte).
    private static UUID idDe(String username) {
        return UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8));
    }

    private void authentifierComme(UtilisateurConnecte principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PersonnageResponse reponse(String nom) {
        return new PersonnageResponse(personnageId, nom, 15, 15, 0, 20, 20,
                List.of(), null, 17, null, false, List.of());
    }

    private CreerPersonnageRequest requeteCreation(List<String> disciplines) {
        CreerPersonnageRequest request = new CreerPersonnageRequest();
        request.setNom("Loup Solitaire");
        request.setDisciplines(disciplines);
        request.setHasardHabilite(5);
        request.setHasardEndurance(3);
        return request;
    }

    // =========================================================
    // POST /personnages (creer)
    // =========================================================

    @Test
    void creerRenvoie201AvecLePersonnageCree() throws Exception {
        List<IdDiscipline> disciplines = List.of(IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE,
                IdDiscipline.SIXIEME_SENS, IdDiscipline.ORIENTATION, IdDiscipline.GUERISON);
        when(partieService.creerPersonnage(marius, "Loup Solitaire", disciplines, 5, 3))
                .thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteCreation(
                                disciplines.stream().map(Enum::name).toList()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Loup Solitaire"));
    }

    @Test
    void creerRenvoie400SiUneDisciplineEstInconnue() throws Exception {
        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteCreation(
                                List.of("CAMOUFLAGE", "CHASSE", "SIXIEME_SENS", "ORIENTATION", "TELEPORTATION")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Discipline inconnue : TELEPORTATION"));

        verifyNoInteractions(partieService);
    }

    @Test
    void creerRenvoie400SiLeNombreDeDisciplinesEstIncorrect() throws Exception {
        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteCreation(List.of("CAMOUFLAGE", "CHASSE")))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(partieService);
    }

    // =========================================================
    // Lectures
    // =========================================================

    @Test
    void listerRenvoieLesPersonnagesDeL_utilisateurConnecte() throws Exception {
        when(partieService.listerPersonnages(marius)).thenReturn(List.of(reponse("Loup Solitaire")));

        mockMvc.perform(get("/personnages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Loup Solitaire"));
    }

    @Test
    void recupererRenvoieLaFicheDuPersonnage() throws Exception {
        when(partieService.recupererPersonnage(personnageId, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapitreActuelId").value(17));
    }

    @Test
    void recupererRenvoie404SiLePersonnageEstIntrouvable() throws Exception {
        when(partieService.recupererPersonnage(personnageId, marius))
                .thenThrow(new RessourceNonTrouveeException("Personnage introuvable : " + personnageId));

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isNotFound());
    }

    @Test
    void recupererRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        when(partieService.recupererPersonnage(personnageId, marius))
                .thenThrow(new AccesRefuseException("Ce personnage ne vous appartient pas"));

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isForbidden());
    }

    @Test
    void chapitreCourantRenvoieLeChapitreActuelDuPersonnage() throws Exception {
        when(partieService.chapitreCourant(personnageId, marius)).thenReturn(
                new ChapitreResponse(17, "Texte du chapitre", false, 4, List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/personnages/{id}/chapitre", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.tirageHasard").value(4));
    }

    @Test
    void historiqueRenvoieLesChapitresDansL_ordreDuService() throws Exception {
        when(partieService.historique(personnageId, marius)).thenReturn(List.of(
                new ChapitreParcouruResponse(85, "Plus recent", false, false, false, false),
                new ChapitreParcouruResponse(1, "Plus ancien", false, false, false, false)));

        mockMvc.perform(get("/personnages/{id}/historique", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chapitreId").value(85))
                .andExpect(jsonPath("$[1].chapitreId").value(1));
    }

    // =========================================================
    // Actions
    // =========================================================

    @Test
    void avancerVersChapitreTransmetLeChapitreCible() throws Exception {
        when(partieService.avancerVersChapitre(personnageId, 85, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/chapitre/{cible}", personnageId, 85))
                .andExpect(status().isOk());

        verify(partieService).avancerVersChapitre(personnageId, 85, marius);
    }

    @Test
    void avancerVersChapitreRenvoie400SiLeLienEstInvalide() throws Exception {
        when(partieService.avancerVersChapitre(personnageId, 999, marius))
                .thenThrow(new IllegalArgumentException("Aucun lien vers le chapitre 999"));

        mockMvc.perform(post("/personnages/{id}/chapitre/{cible}", personnageId, 999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Aucun lien vers le chapitre 999"));
    }

    @Test
    void revenirApresDefaiteUtiliseLaRouteLitteraleEtNonLeChapitreCible() throws Exception {
        when(partieService.revenirApresDefaite(personnageId, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/chapitre/revenir-apres-defaite", personnageId))
                .andExpect(status().isOk());

        verify(partieService).revenirApresDefaite(personnageId, marius);
        verify(partieService, never()).avancerVersChapitre(any(), any(), any());
    }

    @Test
    void ressusciterDelegueAuService() throws Exception {
        when(partieService.ressusciter(personnageId, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/ressusciter", personnageId))
                .andExpect(status().isOk());
    }

    @Test
    void ajouterObjetRamasseL_objetDuChapitre() throws Exception {
        when(partieService.ramasserObjet(personnageId, "repas", marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isOk());
    }

    @Test
    void ajouterObjetRenvoie404SiL_objetEstIntrouvable() throws Exception {
        when(partieService.ramasserObjet(personnageId, "inconnu", marius))
                .thenThrow(new RessourceNonTrouveeException("Objet introuvable : inconnu"));

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}", personnageId, "inconnu"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retirerObjetUtiliseUneQuantiteParDefautDeUn() throws Exception {
        when(partieService.retirerObjet(personnageId, "repas", 1, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(delete("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isOk());

        verify(partieService).retirerObjet(personnageId, "repas", 1, marius);
    }

    @Test
    void retirerObjetUtiliseLaQuantiteFournieEnParametre() throws Exception {
        when(partieService.retirerObjet(personnageId, "repas", 3, marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(delete("/personnages/{id}/objets/{objetId}", personnageId, "repas")
                        .param("quantite", "3"))
                .andExpect(status().isOk());

        verify(partieService).retirerObjet(personnageId, "repas", 3, marius);
    }

    @Test
    void retirerObjetRenvoie400SiLePersonnageNeLePossedePas() throws Exception {
        when(partieService.retirerObjet(eq(personnageId), eq("repas"), anyInt(), eq(marius)))
                .thenThrow(new IllegalArgumentException("Le personnage ne possede pas repas, impossible d'en retirer"));

        mockMvc.perform(delete("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le personnage ne possede pas repas, impossible d'en retirer"));
    }

    @Test
    void consommerObjetDelegueAuService() throws Exception {
        when(partieService.consommerObjet(personnageId, "potion_de_soin", marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}/consommer", personnageId, "potion_de_soin"))
                .andExpect(status().isOk());
    }

    @Test
    void resoudreVolDelegueAuService() throws Exception {
        when(partieService.resoudreVol(personnageId, "poignard", marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/vol/{objetId}", personnageId, "poignard"))
                .andExpect(status().isOk());
    }

    @Test
    void echangerObjetTransmetLesDeuxObjetsDansLeBonOrdre() throws Exception {
        when(partieService.echangerObjet(personnageId, "marteau", "baton", marius)).thenReturn(reponse("Loup Solitaire"));

        mockMvc.perform(post("/personnages/{id}/objets/{a}/echanger-contre/{b}", personnageId, "marteau", "baton"))
                .andExpect(status().isOk());

        verify(partieService).echangerObjet(personnageId, "marteau", "baton", marius);
    }

    @Test
    void supprimerRenvoie204() throws Exception {
        mockMvc.perform(delete("/personnages/{id}", personnageId))
                .andExpect(status().isNoContent());

        verify(partieService).supprimerPersonnage(personnageId, marius);
    }

    @Test
    void actionSimultaneeRenvoie409() throws Exception {
        when(partieService.ramasserObjet(eq(personnageId), anyString(), eq(marius)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Personnage", personnageId));

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isConflict());
    }
}