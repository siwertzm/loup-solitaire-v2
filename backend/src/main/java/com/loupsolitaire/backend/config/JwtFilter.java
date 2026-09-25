package com.loupsolitaire.backend.config;

import java.io.IOException;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

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

                // Recharge l'utilisateur par son identifiant : si le compte a
                // ete supprime entre-temps, UsernameNotFoundException -> la
                // requete continue sans authentification.
                UserDetails userDetails = userDetailsService.loadUserById(utilisateurId);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token invalide/expire/malforme, ou utilisateur supprime : on laisse
            // la requete continuer sans authentification. Spring Security la
            // rejettera avec 401 si la route necessite d'etre authentifie.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}