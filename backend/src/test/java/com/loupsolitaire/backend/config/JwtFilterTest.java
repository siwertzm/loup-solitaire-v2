package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
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
        verify(userDetailsService, never()).loadUserByUsername(anyString());
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
        when(jwtUtil.extractUsername("un-token-valide")).thenReturn("marius");

        UserDetails userDetails = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("marius")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("un-token-valide", userDetails)).thenReturn(true);

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("marius");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neAuthentifiePasSiLeTokenEstInvalide() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer un-token-invalide");
        when(jwtUtil.extractUsername("un-token-invalide")).thenReturn("marius");

        UserDetails userDetails = User.builder()
                .username("marius").password("hash").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("marius")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("un-token-invalide", userDetails)).thenReturn(false);

        creerFiltre().doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void neReleveAucuneExceptionSiLeTokenEstMalformeOuExpire() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-corrompu");
        when(jwtUtil.extractUsername("token-corrompu"))
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
        when(jwtUtil.extractUsername("un-token-valide")).thenReturn("marius");

        creerFiltre().doFilterInternal(request, response, filterChain);

        // L'authentification existante n'a pas ete remplacee.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(dejaLa);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }
}