package com.loupsolitaire.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.json.JsonMapper;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.CompteNonVerifieException;
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.exception.GlobalExceptionHandler;
import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.AuthService;
import com.loupsolitaire.backend.service.CompteService;
import com.loupsolitaire.backend.service.EmailVerificationService;
import com.loupsolitaire.backend.service.PasswordResetService;

/**
 * Couche HTTP uniquement : routes, validation des requetes, codes de retour
 * et parametres transmis aux services. Les regles (conflits, compte non
 * verifie, mot de passe...) sont testees dans AuthServiceTest et
 * CompteServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String DEEP_LINK = "loupsolitaire://auth/login?emailVerified=true";

    @Mock
    private AuthService authService;
    @Mock
    private CompteService compteService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    // Jackson 3 : WRITE_DATES_AS_TIMESTAMPS est desactive par defaut,
    // LocalDate se serialise donc directement en "1997-05-12".
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final UtilisateurConnecte marius = new UtilisateurConnecte(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        // standaloneSetup : pas de contexte Spring, @Value n'est pas injecte.
        ReflectionTestUtils.setField(controller, "mobileLoginUrl", DEEP_LINK);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper), new StringHttpMessageConverter())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                marius, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private UtilisateurResponse profil(String username, String email) {
        return new UtilisateurResponse(marius.id(), username, email, LocalDate.of(1997, 5, 12), true,
                Instant.parse("2026-09-01T10:00:00Z"), List.of());
    }

    private String json(Object objet) {
        return objectMapper.writeValueAsString(objet);
    }

    // =========================================================
    // register
    // =========================================================

    @Test
    void registerRenvoie201AvecLeCompteCree() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");
        request.setDateNaissance(LocalDate.of(1997, 5, 12));
        when(compteService.inscrire(any())).thenReturn(profil("marius", "marius@example.com"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("marius"))
                .andExpect(jsonPath("$.dateNaissance").value("1997-05-12"));
    }

    @Test
    void registerTransmetUnEmailNormalise() throws Exception {
        // JSON ecrit a la main : passer par RegisterRequest.setEmail
        // normaliserait deja l'email avant l'envoi.
        String json = """
                {
                  "username": "marius",
                  "email": "  Marius@Example.COM ",
                  "password": "motdepasse123"
                }
                """;
        when(compteService.inscrire(any())).thenReturn(profil("marius", "marius@example.com"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        ArgumentCaptor<RegisterRequest> requete = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(compteService).inscrire(requete.capture());
        assertThat(requete.getValue().getEmail()).isEqualTo("marius@example.com");
    }

    @Test
    void registerRenvoie409EnCasDeConflit() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");
        when(compteService.inscrire(any())).thenThrow(new ConflitException("Cet email est deja utilise"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cet email est deja utilise"));
    }

    @Test
    void registerRefuseUnUsernameContenantUnArobase() throws Exception {
        RegisterRequest request = new RegisterRequest();
        // Le nom reprend l'email d'un autre joueur : refuse avant tout appel.
        request.setUsername("victime@example.com");
        request.setEmail("pirate@example.com");
        request.setPassword("motdepasse123");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.username")
                        .value("Le nom d'utilisateur ne peut pas contenir le caractere @"));

        verifyNoInteractions(compteService);
    }

    @Test
    void registerRefuseUnMotDePasseTropCourt() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("court");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.password").exists());

        verifyNoInteractions(compteService);
    }

    // =========================================================
    // verify-email / resend-verification
    // =========================================================

    @Test
    void verifyEmailValidePuisRedirigeVersApplication() throws Exception {
        mockMvc.perform(get("/auth/verify-email").param("token", "un-token-valide"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", DEEP_LINK));

        verify(emailVerificationService).verifier("un-token-valide");
    }

    @Test
    void verifyEmailRenvoie401SurUnTokenInvalide() throws Exception {
        doThrow(new TokenInvalideException("Lien de confirmation invalide"))
                .when(emailVerificationService).verifier("mauvais-token");

        mockMvc.perform(get("/auth/verify-email").param("token", "mauvais-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendVerificationRenvoieToujours202() throws Exception {
        mockMvc.perform(post("/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"Marius@Example.com\"}"))
                .andExpect(status().isAccepted());

        verify(compteService).renvoyerVerification("marius@example.com");
    }

    // =========================================================
    // login / refresh / logout
    // =========================================================

    @Test
    void loginRenvoieLesTokens() throws Exception {
        when(authService.connecter("marius", "motdepasse123"))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\": \"marius\", \"password\": \"motdepasse123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginRenvoie403PourUnCompteNonVerifie() throws Exception {
        when(authService.connecter("marius", "motdepasse123"))
                .thenThrow(new CompteNonVerifieException("Merci de confirmer ton adresse email"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\": \"marius\", \"password\": \"motdepasse123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Compte non verifie"));
    }

    @Test
    void loginRenvoie401SurDeMauvaisIdentifiants() throws Exception {
        when(authService.connecter("marius", "mauvais"))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\": \"marius\", \"password\": \"mauvais\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nom d'utilisateur ou mot de passe incorrect"));
    }

    @Test
    void refreshRenvoieUnNouveauCoupleDeTokens() throws Exception {
        when(authService.rafraichir("ancien-refresh-token"))
                .thenReturn(new AuthResponse("nouveau-access-token", "nouveau-refresh-token"));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"ancien-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("nouveau-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("nouveau-refresh-token"));
    }

    @Test
    void refreshRenvoie401SurUnTokenInvalide() throws Exception {
        when(authService.rafraichir("token-invalide"))
                .thenThrow(new TokenInvalideException("Session invalide, merci de vous reconnecter"));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"token-invalide\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevoqueLeTokenEtRenvoie204() throws Exception {
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"un-refresh-token\"}"))
                .andExpect(status().isNoContent());

        verify(authService).deconnecter("un-refresh-token");
    }

    // =========================================================
    // Mot de passe oublie
    // =========================================================

    @Test
    void forgotPasswordRenvoieToujours202() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"marius@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).demanderReinitialisation("marius@example.com");
    }

    @Test
    void verifyResetCodeRenvoieLeJetonDeReinitialisation() throws Exception {
        when(passwordResetService.verifierCode("marius@example.com", "123456")).thenReturn("jeton-reset");

        mockMvc.perform(post("/auth/verify-reset-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"marius@example.com\", \"code\": \"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("jeton-reset"));
    }

    @Test
    void resetPasswordRenvoie204() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\": \"jeton-reset\", \"newPassword\": \"nouveaumotdepasse\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).reinitialiser("jeton-reset", "nouveaumotdepasse");
    }

    // =========================================================
    // /me
    // =========================================================

    @Test
    void getCurrentUserRenvoieLeProfilDeL_utilisateurConnecte() throws Exception {
        authentifier();
        when(compteService.profil(marius)).thenReturn(profil("marius", "marius@example.com"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("marius"))
                .andExpect(jsonPath("$.email").value("marius@example.com"));
    }

    @Test
    void updateProfilTransmetLaRequeteDeL_utilisateurConnecte() throws Exception {
        authentifier();
        when(compteService.modifierProfil(any(), any())).thenReturn(profil("marius", "nouveau@example.com"));

        mockMvc.perform(put("/auth/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"Nouveau@Example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nouveau@example.com"));

        ArgumentCaptor<UpdateProfilRequest> requete = ArgumentCaptor.forClass(UpdateProfilRequest.class);
        verify(compteService).modifierProfil(org.mockito.ArgumentMatchers.eq(marius), requete.capture());
        assertThat(requete.getValue().getEmail()).isEqualTo("nouveau@example.com");
    }

    @Test
    void updateProfilRefuseUnUsernameContenantUnArobase() throws Exception {
        authentifier();

        mockMvc.perform(put("/auth/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"victime@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.username")
                        .value("Le nom d'utilisateur ne peut pas contenir le caractere @"));

        verifyNoInteractions(compteService);
    }

    @Test
    void updateProfilRenvoie409SiL_emailEstDejaUtilise() throws Exception {
        authentifier();
        when(compteService.modifierProfil(any(), any())).thenThrow(new ConflitException("Cet email est deja utilise"));

        mockMvc.perform(put("/auth/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"prisparunautre@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void changePasswordRenvoieUneNouvelleSession() throws Exception {
        authentifier();
        when(compteService.changerMotDePasse(marius, "ancienmotdepasse", "nouveaumotdepasse"))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(put("/auth/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"ancienmotdepasse\", \"newPassword\": \"nouveaumotdepasse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void changePasswordRenvoie401SiLeMotDePasseActuelEstFaux() throws Exception {
        authentifier();
        when(compteService.changerMotDePasse(marius, "faux", "nouveaumotdepasse"))
                .thenThrow(new BadCredentialsException("Mot de passe actuel incorrect"));

        mockMvc.perform(put("/auth/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"faux\", \"newPassword\": \"nouveaumotdepasse\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAccountRenvoie204() throws Exception {
        authentifier();

        mockMvc.perform(delete("/auth/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"motdepasse123\"}"))
                .andExpect(status().isNoContent());

        verify(compteService).supprimerCompte(marius, "motdepasse123");
    }
}