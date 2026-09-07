package com.loupsolitaire.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.loupsolitaire.backend.config.JwtUtil;
import com.loupsolitaire.backend.exception.GlobalExceptionHandler;
import com.loupsolitaire.backend.exception.TokenInvalideException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.AuthRequest;
import com.loupsolitaire.backend.request.RefreshRequest;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.ResendVerificationRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.service.EmailVerificationService;
import com.loupsolitaire.backend.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    // WRITE_DATES_AS_TIMESTAMPS desactive : sinon LocalDate se serialise en
    // tableau [1997,5,12] au lieu de "1997-05-12", contrairement au vrai
    // comportement de l'application (configure automatiquement par Spring Boot).
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        // setup "standalone" : controleur isole + le vrai GlobalExceptionHandler,
        // pour verifier les codes HTTP de bout en bout sans charger tout Spring.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper), new StringHttpMessageConverter())
                .build();
    }

    @AfterEach
    void tearDown() {
        // Le SecurityContextHolder est un ThreadLocal statique : sans ce nettoyage,
        // l'authentification d'un test pourrait "fuiter" vers le suivant.
        SecurityContextHolder.clearContext();
    }

    // En setup standalone (sans la vraie chaine de filtres Spring Security),
    // SecurityMockMvcRequestPostProcessors.user(...) ne suffit pas a alimenter
    // @AuthenticationPrincipal : on peuple nous-memes le SecurityContextHolder.
    private void authentifierComme(UserDetails principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================
    // register
    // =========================================================

    @Test
    void registerCreeUnCompteEtDeclencheL_envoiDuLienDeVerification() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");

        when(utilisateurRepository.existsByUsername("marius")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("marius@example.com")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("marius"))
                .andExpect(jsonPath("$.email").value("marius@example.com"))
                .andExpect(jsonPath("$.emailVerifie").value(false));

        verify(emailVerificationService).envoyerLienDeVerification(any(Utilisateur.class));
    }

    @Test
    void registerRefuseUnUsernameDejaPris() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");

        when(utilisateurRepository.existsByUsername("marius")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nom d'utilisateur deja utilise"));

        verify(emailVerificationService, never()).envoyerLienDeVerification(any());
    }

    @Test
    void registerRefuseUnEmailDejaPris() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");

        when(utilisateurRepository.existsByUsername("marius")).thenReturn(false);
        when(utilisateurRepository.existsByEmail("marius@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cet email est deja utilise"));
    }

    // =========================================================
    // login
    // =========================================================

    @Test
    void loginRenvoieLesTokensQuandLeCompteEstVerifie() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setIdentifiant("marius");
        request.setPassword("motdepasse123");

        UserDetails userDetails = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmailVerifie(true);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        when(jwtUtil.generateToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.creerToken(utilisateur)).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void loginBloqueUnCompteNonVerifie() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setIdentifiant("marius");
        request.setPassword("motdepasse123");

        UserDetails userDetails = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmailVerifie(false);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Compte non verifie"));

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void loginRenvoie401SurDeMauvaisIdentifiants() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setIdentifiant("marius");
        request.setPassword("mauvais-mot-de-passe");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nom d'utilisateur ou mot de passe incorrect"));
    }

    // =========================================================
    // verify-email
    // =========================================================

    @Test
    void verifyEmailRenvoieUnePageDeConfirmation() throws Exception {
        mockMvc.perform(get("/auth/verify-email").param("token", "un-token-valide"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Email confirme")));

        verify(emailVerificationService).verifier("un-token-valide");
    }

    @Test
    void verifyEmailRenvoie401SurUnTokenInvalide() throws Exception {
        doThrow(new TokenInvalideException("Lien de confirmation invalide"))
                .when(emailVerificationService).verifier("mauvais-token");

        mockMvc.perform(get("/auth/verify-email").param("token", "mauvais-token"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // resend-verification
    // =========================================================

    @Test
    void resendVerificationRenvoieToujours202MemeSiLeCompteEstDejaVerifie() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("marius@example.com");

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmailVerifie(true);
        when(utilisateurRepository.findByEmail("marius@example.com")).thenReturn(Optional.of(utilisateur));

        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(emailVerificationService, never()).envoyerLienDeVerification(any());
    }

    @Test
    void resendVerificationRenvoie202MemeSiL_emailEstInconnu() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("inconnu@example.com");

        when(utilisateurRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    // =========================================================
    // refresh
    // =========================================================

    @Test
    void refreshRenvoieUnNouveauCoupleDeTokens() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("ancien-refresh-token");

        RefreshTokenService.RotationResult resultat =
                new RefreshTokenService.RotationResult("marius", "hash", "nouveau-refresh-token");
        when(refreshTokenService.validerEtPivoter("ancien-refresh-token")).thenReturn(resultat);
        when(jwtUtil.generateToken(any())).thenReturn("nouveau-access-token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("nouveau-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("nouveau-refresh-token"));
    }

    @Test
    void refreshRenvoie401SurUnTokenInvalide() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token-vole");

        when(refreshTokenService.validerEtPivoter("token-vole"))
                .thenThrow(new TokenInvalideException("Reutilisation detectee"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // logout
    // =========================================================

    @Test
    void logoutRevoqueLeTokenEtRenvoie204() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token-a-revoquer");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(refreshTokenService).revoquer("token-a-revoquer");
    }

    // =========================================================
    // GET /me
    // =========================================================

    @Test
    void getCurrentUserRenvoieLeProfilDeL_utilisateurConnecte() throws Exception {
        UserDetails principal = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(UUID.randomUUID());
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setEmailVerifie(true);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        authentifierComme(principal);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("marius"))
                .andExpect(jsonPath("$.email").value("marius@example.com"));
    }

    // =========================================================
    // PUT /me
    // =========================================================

    @Test
    void updateProfilChangeL_emailEtRedemandeUneVerification() throws Exception {
        UserDetails principal = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmail("ancien@example.com");
        utilisateur.setEmailVerifie(true);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.existsByEmail("nouveau@example.com")).thenReturn(false);

        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setEmail("nouveau@example.com");
        authentifierComme(principal);

        mockMvc.perform(put("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nouveau@example.com"))
                .andExpect(jsonPath("$.emailVerifie").value(false));

        verify(emailVerificationService).envoyerLienDeVerification(utilisateur);
    }

    @Test
    void updateProfilRefuseUnEmailDejaUtiliseParUnAutreCompte() throws Exception {
        UserDetails principal = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmail("ancien@example.com");
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.existsByEmail("prisparunautre@example.com")).thenReturn(true);

        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setEmail("prisparunautre@example.com");
        authentifierComme(principal);

        mockMvc.perform(put("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(emailVerificationService, never()).envoyerLienDeVerification(any());
    }

    @Test
    void updateProfilMetAJourUniquementLaDateDeNaissance() throws Exception {
        UserDetails principal = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setEmailVerifie(true);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setDateNaissance(LocalDate.of(1997, 5, 12));
        authentifierComme(principal);

        mockMvc.perform(put("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateNaissance").value("1997-05-12"))
                // l'email n'ayant pas change, la verification ne doit pas etre redemandee.
                .andExpect(jsonPath("$.emailVerifie").value(true));

        verify(emailVerificationService, never()).envoyerLienDeVerification(any());
    }
}