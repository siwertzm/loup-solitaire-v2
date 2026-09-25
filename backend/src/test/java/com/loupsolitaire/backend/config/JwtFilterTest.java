package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private static final UUID UTILISATEUR_ID = UUID.randomUUID();

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    // JwtFilter utilise @RequiredArgsConstructor (champs final) : pas de
    // @InjectMocks ici, on construit l'instance nous-memes dans chaque test.
    private JwtFilter creerFiltre() {
        return new JwtFilter(jwtUtil, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void laisseContinuerLaRequeteSansHeaderAuthorization() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserById(any());
    }

    @Test
    void laisseContinuerLaRequeteSiLeHeaderNeCommencePasParBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authentifieLeContexteQuandLeTokenEstValide() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-valide");
        when(jwtUtil.extractUtilisateurId("un-token-valide")).thenReturn(UTILISATEUR_ID);

        UserDetails userDetails = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserById(UTILISATEUR_ID)).thenReturn(userDetails);

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("marius");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void lePrincipalPorteLeNomActuelApresUnRenommage() throws Exception {
        // Le token a ete emis quand l'utilisateur s'appelait encore "bob" :
        // il ne contient que son UUID, donc le principal reflete le nom
        // ACTUEL lu en base ("bob2"), et jamais l'ancien nom.
        when(request.getHeader("Authorization")).thenReturn("Bearer token-emis-avant-renommage");
        when(jwtUtil.extractUtilisateurId("token-emis-avant-renommage")).thenReturn(UTILISATEUR_ID);

        UserDetails userDetails = User.builder()
                .username("bob2").password("hash").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserById(UTILISATEUR_ID)).thenReturn(userDetails);

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("bob2");
    }

    @Test
    void neAuthentifiePasSiLeTokenEstExpire() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-expire");
        when(jwtUtil.extractUtilisateurId("un-token-expire"))
                .thenThrow(new ExpiredJwtException(null, null, "token expire"));

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserById(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neAuthentifiePasSiLUtilisateurAEteSupprime() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-valide");
        when(jwtUtil.extractUtilisateurId("un-token-valide")).thenReturn(UTILISATEUR_ID);
        when(userDetailsService.loadUserById(UTILISATEUR_ID))
                .thenThrow(new UsernameNotFoundException("Utilisateur introuvable"));

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neReleveAucuneExceptionSiLeTokenEstMalforme() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-corrompu");
        when(jwtUtil.extractUtilisateurId("token-corrompu"))
                .thenThrow(new RuntimeException("token illisible"));

        // Ne doit jamais propager l'exception : la requete doit quand meme
        // continuer sans authentification (Spring Security la rejettera
        // plus loin si la route l'exige).
        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neReecrasePasUneAuthentificationDejaPresente() throws Exception {
        UsernamePasswordAuthenticationToken dejaLa =
                new UsernamePasswordAuthenticationToken("quelqu-un", null);
        SecurityContextHolder.getContext().setAuthentication(dejaLa);

        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-valide");

        creerFiltre().doFilterInternal(request, response, filterChain);

        // L'authentification existante n'a pas ete remplacee.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(dejaLa);
        verify(jwtUtil, never()).extractUtilisateurId(any());
        verify(userDetailsService, never()).loadUserById(any());
        verify(filterChain).doFilter(request, response);
    }
}