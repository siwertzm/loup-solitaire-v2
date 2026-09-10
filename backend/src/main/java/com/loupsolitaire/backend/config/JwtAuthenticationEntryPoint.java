package com.loupsolitaire.backend.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loupsolitaire.backend.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Sans ce bean, Spring Security utilise son AuthenticationEntryPoint par
 * defaut (Http403ForbiddenEntryPoint) des qu'aucune authentification n'est
 * presente : toute requete non authentifiee recoit alors un 403, pas un 401.
 *
 * Cote client, l'intercepteur HTTP ne tente un refresh silencieux que sur un
 * 401 (le 403 est reserve a "authentifie mais pas le droit", cf.
 * AccesRefuseException). Sans ce bean, un access token expire/absent renvoie
 * donc un 403 brut : pas de refresh, pas de redirection propre vers /login.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Non authentifie",
                "Jeton d'acces manquant, invalide ou expire"
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}