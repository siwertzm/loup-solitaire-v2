package com.loupsolitaire.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.request.JouerTourCombatRequest;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.service.PartieService;

/**
 * Couche HTTP uniquement : routes, codes de retour, traduction de l'action
 * et des erreurs. La logique du combat est testee dans CombatServiceTest et
 * PartieServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class CombatControllerTest {

    @Mock
    private PartieService partieService;

    @InjectMocks
    private CombatController controller;

    private MockMvc mockMvc;
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final UUID personnageId = UUID.randomUUID();
    private final UtilisateurConnecte marius =
            new UtilisateurConnecte(UUID.nameUUIDFromBytes("marius".getBytes(StandardCharsets.UTF_8)));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                marius, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CombatResponse reponse(String statut) {
        return new CombatResponse(UUID.randomUUID(), 17, List.of(), 0, false, 0, statut, false, null);
    }

    private JouerTourCombatRequest tour(String action, String objetId) {
        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction(action);
        request.setObjetId(objetId);
        return request;
    }

    // =========================================================
    // POST /combat (initier) et GET /combat
    // =========================================================

    @Test
    void initierRenvoie201() throws Exception {
        when(partieService.initierCombat(personnageId, marius)).thenReturn(reponse("EN_COURS"));

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void initierRenvoie400SiLeChapitreN_estPasUnCombat() throws Exception {
        when(partieService.initierCombat(personnageId, marius))
                .thenThrow(new IllegalArgumentException("Le chapitre 17 n'est pas un combat"));

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initierRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        when(partieService.initierCombat(personnageId, marius))
                .thenThrow(new AccesRefuseException("Ce personnage ne vous appartient pas"));

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isForbidden());
    }

    @Test
    void recupererRenvoieLeCombatEnCours() throws Exception {
        when(partieService.combatActuel(personnageId, marius)).thenReturn(reponse("EN_COURS"));

        mockMvc.perform(get("/personnages/{id}/combat", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapitreId").value(17));
    }

    @Test
    void recupererRenvoie404SiAucunCombatN_estEnCours() throws Exception {
        when(partieService.combatActuel(personnageId, marius))
                .thenThrow(new RessourceNonTrouveeException("Aucun combat en cours sur le chapitre 17"));

        mockMvc.perform(get("/personnages/{id}/combat", personnageId))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // POST /combat/tour
    // =========================================================

    @Test
    void jouerTourTraduitL_actionSansTenirCompteDeLaCasse() throws Exception {
        when(partieService.jouerTour(personnageId, ActionCombat.ATTAQUE, null, marius))
                .thenReturn(reponse("EN_COURS"));

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour(" attaque ", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void jouerTourTransmetL_objetUtilise() throws Exception {
        when(partieService.jouerTour(personnageId, ActionCombat.OBJET, "potion_de_soin", marius))
                .thenReturn(reponse("EN_COURS"));

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour("OBJET", "potion_de_soin"))))
                .andExpect(status().isOk());

        verify(partieService).jouerTour(personnageId, ActionCombat.OBJET, "potion_de_soin", marius);
    }

    @Test
    void jouerTourRenvoie404SiL_objetEstIntrouvable() throws Exception {
        when(partieService.jouerTour(personnageId, ActionCombat.OBJET, "objet-inconnu", marius))
                .thenThrow(new RessourceNonTrouveeException("Objet introuvable : objet-inconnu"));

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour("OBJET", "objet-inconnu"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void jouerTourRenvoie400SiL_actionEstInconnue() throws Exception {
        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour("DANSER", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Action de combat inconnue : DANSER"));

        verifyNoInteractions(partieService);
    }

    @Test
    void jouerTourRenvoie400SiL_actionEstVide() throws Exception {
        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour("", null))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(partieService);
    }

    @Test
    void jouerTourRenvoie409SiUneAutreActionVientD_etreAppliquee() throws Exception {
        // Double tap sur "Attaque" : la seconde requete arrive sur une version
        // perimee du personnage et doit etre refusee, pas jouee une 2e fois.
        when(partieService.jouerTour(personnageId, ActionCombat.ATTAQUE, null, marius))
                .thenThrow(new ObjectOptimisticLockingFailureException(Personnage.class, personnageId));

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tour("attaque", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Action simultanee"));
    }
}