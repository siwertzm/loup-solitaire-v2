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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    // JwtFilter utilise @RequiredArgsConstructor (champs final) : pas de
    // @InjectMocks ici, on construit l'instance nous-memes dans chaque test.
    // Plus aucune dependance vers la base : seul JwtUtil est necessaire.
    private JwtFilter creerFiltre() {
        return new JwtFilter(jwtUtil);
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
        verify(jwtUtil, never()).extractUtilisateurId(any());
    }

    @Test
    void laisseContinuerLaRequeteSiLeHeaderNeCommencePasParBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authentifieAvecL_identifiantDuTokenSansInterrogerLaBase() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-valide");
        when(jwtUtil.extractUtilisateurId("un-token-valide")).thenReturn(UTILISATEUR_ID);

        creerFiltre().doFilterInternal(request, response, filterChain);

        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentification).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentification.getPrincipal()).isEqualTo(new UtilisateurConnecte(UTILISATEUR_ID));
        assertThat(authentification.getName()).isEqualTo(UTILISATEUR_ID.toString());
        assertThat(authentification.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neAuthentifiePasSiLeTokenEstExpire() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-expire");
        when(jwtUtil.extractUtilisateurId("un-token-expire"))
                .thenThrow(new ExpiredJwtException(null, null, "token expire"));

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
        verify(filterChain).doFilter(request, response);
    }
}