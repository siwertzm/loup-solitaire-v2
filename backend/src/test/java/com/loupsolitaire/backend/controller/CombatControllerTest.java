package com.loupsolitaire.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loupsolitaire.backend.exception.GlobalExceptionHandler;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.ActionCombat;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.request.JouerTourCombatRequest;
import com.loupsolitaire.backend.response.CombatResponse;
import com.loupsolitaire.backend.service.CombatService;
import com.loupsolitaire.backend.service.mapper.CombatMapper;
import com.loupsolitaire.backend.service.record.TourJoue;

@ExtendWith(MockitoExtension.class)
class CombatControllerTest {

    @Mock
    private CombatService combatService;
    @Mock
    private CombatMapper combatMapper;
    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private ObjetRepository objetRepository;

    @InjectMocks
    private CombatController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final UUID personnageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifierComme(String username) {
        UserDetails principal = User.builder()
                .username(username).password("hash").authorities("ROLE_USER").build();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Personnage creerPersonnage(String proprietaire) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(proprietaire);

        Chapitre chapitre = new Chapitre();
        chapitre.setId(17);

        Personnage personnage = new Personnage();
        personnage.setId(personnageId);
        personnage.setUtilisateur(utilisateur);
        personnage.setChapitreActuel(chapitre);
        return personnage;
    }

    private CombatResponse reponseVide() {
        return new CombatResponse(UUID.randomUUID(), 17, List.of(), 0, false, 0, "EN_COURS", false, null);
    }

    // =========================================================
    // POST /personnages/{id}/combat (initier)
    // =========================================================

    @Test
    void initierDemarreLeCombatEtRenvoie201() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Combat combat = new Combat();

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.initierCombat(personnage)).thenReturn(combat);
        when(combatMapper.versReponse(combat)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void initierRenvoie404SiLePersonnageEstIntrouvable() throws Exception {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.empty());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isNotFound());

        verify(combatService, never()).initierCombat(any());
    }

    @Test
    void initierRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        Personnage personnage = creerPersonnage("quelqu-un-d-autre");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat", personnageId))
                .andExpect(status().isForbidden());

        verify(combatService, never()).initierCombat(any());
    }

    // =========================================================
    // GET /personnages/{id}/combat (recuperer)
    // =========================================================

    @Test
    void recupererRenvoieLeCombatEnCours() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Combat combat = new Combat();

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.of(combat));
        when(combatMapper.versReponse(combat)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}/combat", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void recupererRenvoie404SiAucunCombatN_estEnCours() throws Exception {
        Personnage personnage = creerPersonnage("marius");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.combatActuel(personnage)).thenReturn(Optional.empty());

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}/combat", personnageId))
                .andExpect(status().isNotFound());
    }

    @Test
    void recupererRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        Personnage personnage = creerPersonnage("quelqu-un-d-autre");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}/combat", personnageId))
                .andExpect(status().isForbidden());
    }

    // =========================================================
    // POST /personnages/{id}/combat/tour (jouerTour)
    // =========================================================

    @Test
    void jouerTourAvecAttaqueNeRequiertAucunObjet() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Combat combat = new Combat();
        TourJoue tourJoue = new TourJoue(combat, null);

        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("attaque");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(combatService.jouerTour(personnage, ActionCombat.ATTAQUE, null)).thenReturn(tourJoue);
        when(combatMapper.versReponse(combat, null)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        verify(objetRepository, never()).findById(any());
    }

    @Test
    void jouerTourAvecObjetVaChercherL_objetEnBase() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Combat combat = new Combat();
        TourJoue tourJoue = new TourJoue(combat, null);
        Objet potion = new Objet();
        potion.setId("potion_de_soin");

        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("OBJET");
        request.setObjetId("potion_de_soin");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("potion_de_soin")).thenReturn(Optional.of(potion));
        when(combatService.jouerTour(personnage, ActionCombat.OBJET, potion)).thenReturn(tourJoue);
        when(combatMapper.versReponse(eq(combat), any())).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void jouerTourRenvoie404SiL_objetEstIntrouvable() throws Exception {
        Personnage personnage = creerPersonnage("marius");

        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("OBJET");
        request.setObjetId("objet-inconnu");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("objet-inconnu")).thenReturn(Optional.empty());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(combatService, never()).jouerTour(any(), any(), any());
    }

    @Test
    void jouerTourRenvoie400SiL_actionEstInconnue() throws Exception {
        Personnage personnage = creerPersonnage("marius");

        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("TELEPORTATION");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(combatService, never()).jouerTour(any(), any(), any());
    }

    @Test
    void jouerTourRenvoie400SiL_actionEstVide() throws Exception {
        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("");

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(personnageRepository, never()).findById(any());
    }

    @Test
    void jouerTourRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        Personnage personnage = creerPersonnage("quelqu-un-d-autre");
        JouerTourCombatRequest request = new JouerTourCombatRequest();
        request.setAction("ATTAQUE");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/combat/tour", personnageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(combatService, never()).jouerTour(any(), any(), any());
    }
}