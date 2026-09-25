package com.loupsolitaire.backend.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Tout utilisateur authentifie a le meme role : pas de lecture en base.
    private static final List<SimpleGrantedAuthority> ROLES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(HEADER_NAME);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Verifie signature + expiration, et renvoie l'UUID de
                // l'utilisateur (leve une exception si le token n'est pas valide).
                UUID utilisateurId = jwtUtil.extractUtilisateurId(jwt);

                // Aucune requete en base : l'identifiant signe dans le token
                // suffit (voir UtilisateurConnecte). Les controleurs chargent
                // l'utilisateur ou le personnage seulement s'ils en ont besoin.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        new UtilisateurConnecte(utilisateurId), null, ROLES
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token invalide/expire/malforme : on laisse
            // la requete continuer sans authentification. Spring Security la
            // rejettera avec 401 si la route necessite d'etre authentifie.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}