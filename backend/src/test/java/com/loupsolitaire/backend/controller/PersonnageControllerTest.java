package com.loupsolitaire.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.CreerPersonnageRequest;
import com.loupsolitaire.backend.response.ChapitreResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.service.EffetChapitreService;
import com.loupsolitaire.backend.service.InventaireService;
import com.loupsolitaire.backend.service.ObjetService;
import com.loupsolitaire.backend.service.PersonnageService;
import com.loupsolitaire.backend.service.mapper.ChapitreMapper;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;

@ExtendWith(MockitoExtension.class)
class PersonnageControllerTest {

    @Mock
    private PersonnageService personnageService;
    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ObjetRepository objetRepository;
    @Mock
    private InventaireService inventaireService;
    @Mock
    private ObjetService objetService;
    @Mock
    private EffetChapitreService effetChapitreService;
    @Mock
    private PersonnageMapper personnageMapper;
    @Mock
    private ChapitreMapper chapitreMapper;

    @InjectMocks
    private PersonnageController controller;

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

    private PersonnageResponse reponseVide() {
        return new PersonnageResponse(personnageId, "Loup Solitaire", 15, 15, 0, 20, 20,
                List.of(), null, 17, null, false, List.of());
    }

    // =========================================================
    // POST /personnages (creer)
    // =========================================================

    @Test
    void creerRenvoie201AvecLePersonnageCree() throws Exception {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");

        CreerPersonnageRequest request = new CreerPersonnageRequest();
        request.setNom("Loup Solitaire");
        request.setDisciplines(List.of("CAMOUFLAGE", "CHASSE", "SIXIEME_SENS", "ORIENTATION", "GUERISON"));

        Personnage personnage = creerPersonnage("marius");

        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        when(personnageService.creerPersonnage(eq(utilisateur), eq("Loup Solitaire"),
                eq(List.of(IdDiscipline.CAMOUFLAGE, IdDiscipline.CHASSE, IdDiscipline.SIXIEME_SENS,
                        IdDiscipline.ORIENTATION, IdDiscipline.GUERISON))))
                .thenReturn(personnage);
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Loup Solitaire"));
    }

    @Test
    void creerRenvoie400SiUneDisciplineEstInconnue() throws Exception {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        CreerPersonnageRequest request = new CreerPersonnageRequest();
        request.setNom("Loup Solitaire");
        request.setDisciplines(List.of("VOL_DIRECT", "CHASSE", "SIXIEME_SENS", "ORIENTATION", "GUERISON"));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(personnageService, never()).creerPersonnage(any(), any(), any());
    }

    @Test
    void creerRenvoie400SiLeNombreDeDisciplinesEstIncorrect() throws Exception {
        CreerPersonnageRequest request = new CreerPersonnageRequest();
        request.setNom("Loup Solitaire");
        request.setDisciplines(List.of("CAMOUFLAGE", "CHASSE"));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(utilisateurRepository, never()).findByUsername(any());
    }

    // =========================================================
    // GET /personnages (lister)
    // =========================================================

    @Test
    void listerRenvoieLesPersonnagesDeL_utilisateurConnecte() throws Exception {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        Personnage personnage = creerPersonnage("marius");

        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        when(personnageRepository.findByUtilisateur(utilisateur)).thenReturn(List.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(get("/personnages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].nom").value("Loup Solitaire"));
    }

    // =========================================================
    // GET /personnages/{id} (recuperer)
    // =========================================================

    @Test
    void recupererRenvoieLaFicheDuPersonnage() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Loup Solitaire"));
    }

    @Test
    void recupererRenvoie404SiLePersonnageEstIntrouvable() throws Exception {
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.empty());

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isNotFound());
    }

    @Test
    void recupererRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        Personnage personnage = creerPersonnage("quelqu-un-d-autre");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}", personnageId))
                .andExpect(status().isForbidden());
    }

    // =========================================================
    // GET /personnages/{id}/chapitre
    // =========================================================

    @Test
    void chapitreCourantRenvoieLeChapitreActuelDuPersonnage() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        ChapitreResponse reponse = new ChapitreResponse(
                17, "Un texte de chapitre", true, null, List.of(), List.of(), List.of(), List.of());

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(chapitreMapper.versReponse(17, personnage)).thenReturn(reponse);

        authentifierComme("marius");

        mockMvc.perform(get("/personnages/{id}/chapitre", personnageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17));
    }

    // =========================================================
    // POST /personnages/{id}/chapitre/{chapitreCibleId}
    // =========================================================

    @Test
    void avancerVersChapitreDeplaceLePersonnage() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/chapitre/{chapitreCibleId}", personnageId, 85))
                .andExpect(status().isOk());

        verify(personnageService).avancerVersChapitre(personnage, 85);
    }

    // =========================================================
    // POST /personnages/{id}/chapitre/revenir-apres-defaite
    // =========================================================

    @Test
    void revenirApresDefaiteRestaureLePersonnage() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/chapitre/revenir-apres-defaite", personnageId))
                .andExpect(status().isOk());

        verify(personnageService).revenirApresDefaite(personnage);
    }

    // =========================================================
    // POST /personnages/{id}/ressusciter
    // =========================================================

    @Test
    void ressusciterRestaureLePersonnage() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/ressusciter", personnageId))
                .andExpect(status().isOk());

        verify(personnageService).ressusciter(personnage);
    }

    // =========================================================
    // POST /personnages/{id}/objets/{objetId} (ajouterObjet)
    // =========================================================

    @Test
    void ajouterObjetRamasseL_objetDuChapitre() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet objet = new Objet();
        objet.setId("repas");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("repas")).thenReturn(Optional.of(objet));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isOk());

        verify(personnageService).ramasserObjetDuChapitre(personnage, objet);
    }

    @Test
    void ajouterObjetRenvoie404SiL_objetEstIntrouvable() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("inconnu")).thenReturn(Optional.empty());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}", personnageId, "inconnu"))
                .andExpect(status().isNotFound());

        verify(personnageService, never()).ramasserObjetDuChapitre(any(), any());
    }

    // =========================================================
    // DELETE /personnages/{id}/objets/{objetId} (retirerObjet)
    // =========================================================

    @Test
    void retirerObjetUtiliseUneQuantiteParDefautDeUn() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet objet = new Objet();
        objet.setId("repas");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("repas")).thenReturn(Optional.of(objet));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(delete("/personnages/{id}/objets/{objetId}", personnageId, "repas"))
                .andExpect(status().isOk());

        verify(inventaireService).retirerObjet(personnage, objet, 1);
    }

    @Test
    void retirerObjetUtiliseLaQuantiteFournieEnParametre() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet objet = new Objet();
        objet.setId("repas");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("repas")).thenReturn(Optional.of(objet));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(delete("/personnages/{id}/objets/{objetId}", personnageId, "repas")
                        .param("quantite", "3"))
                .andExpect(status().isOk());

        verify(inventaireService).retirerObjet(personnage, objet, 3);
    }

    // =========================================================
    // POST /personnages/{id}/objets/{objetId}/consommer
    // =========================================================

    @Test
    void consommerObjetAppliqueL_effetPuisRetireL_objet() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet objet = new Objet();
        objet.setId("potion_de_soin");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("potion_de_soin")).thenReturn(Optional.of(objet));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/objets/{objetId}/consommer", personnageId, "potion_de_soin"))
                .andExpect(status().isOk());

        verify(objetService).appliquerEffetsConsommation(personnage, objet);
        verify(inventaireService).retirerObjet(personnage, objet, 1);
    }

    // =========================================================
    // POST /personnages/{id}/vol/{objetId} (resoudreVol)
    // =========================================================

    @Test
    void resoudreVolDelegueAuServiceDesEffets() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet objet = new Objet();
        objet.setId("poignard");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("poignard")).thenReturn(Optional.of(objet));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/vol/{objetId}", personnageId, "poignard"))
                .andExpect(status().isOk());

        verify(effetChapitreService).resoudreVolEnAttente(personnage, objet);
    }

    // =========================================================
    // POST /personnages/{id}/objets/{a}/echanger-contre/{b}
    // =========================================================

    @Test
    void echangerObjetRetireUnObjetEtEnAjouteUnAutre() throws Exception {
        Personnage personnage = creerPersonnage("marius");
        Objet marteau = new Objet();
        marteau.setId("marteau");
        Objet baton = new Objet();
        baton.setId("baton");

        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));
        when(objetRepository.findById("marteau")).thenReturn(Optional.of(marteau));
        when(objetRepository.findById("baton")).thenReturn(Optional.of(baton));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponseVide());

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/objets/{a}/echanger-contre/{b}",
                        personnageId, "marteau", "baton"))
                .andExpect(status().isOk());

        verify(personnageService).echangerObjet(personnage, baton, marteau);
    }

    // =========================================================
    // Verification proprietaire partagee (echantillon sur un endpoint POST)
    // =========================================================

    @Test
    void ressusciterRenvoie403SiLePersonnageNAppartientPasAL_utilisateur() throws Exception {
        Personnage personnage = creerPersonnage("quelqu-un-d-autre");
        when(personnageRepository.findById(personnageId)).thenReturn(Optional.of(personnage));

        authentifierComme("marius");

        mockMvc.perform(post("/personnages/{id}/ressusciter", personnageId))
                .andExpect(status().isForbidden());

        verify(personnageService, never()).ressusciter(any());
    }
}