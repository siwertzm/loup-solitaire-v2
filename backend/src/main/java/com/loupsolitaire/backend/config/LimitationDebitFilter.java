package com.loupsolitaire.backend.config;

import java.io.IOException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.loupsolitaire.backend.exception.ErrorResponse;
import com.loupsolitaire.backend.exception.TropDeRequetesException;
import com.loupsolitaire.backend.service.LimiteurDeDebit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limitation de debit par adresse IP sur les routes publiques /auth/**
 * (SEC-02). Les limites par compte ou par email sont appliquees dans les
 * services, qui connaissent l'email (AuthService, PasswordResetService,
 * CompteService).
 *
 * Place avant JwtFilter dans SecurityConfig : une requete refusee ici ne
 * coute ni verification de JWT, ni BCrypt, ni envoi d'email.
 */
@Component
@RequiredArgsConstructor
public class LimitationDebitFilter extends OncePerRequestFilter {

    private static final String PREFIXE_AUTH = "/auth/";

    // Regle specifique par route, en plus de la regle globale AUTH_IP.
    private static final Map<String, String> REGLE_PAR_ROUTE = Map.of(
            "/auth/login", LimiteurDeDebit.LOGIN_IP,
            "/auth/register", LimiteurDeDebit.INSCRIPTION_IP,
            "/auth/forgot-password", LimiteurDeDebit.EMAIL_IP,
            "/auth/verify-reset-code", LimiteurDeDebit.EMAIL_IP,
            "/auth/resend-verification", LimiteurDeDebit.EMAIL_IP,
            "/auth/refresh", LimiteurDeDebit.REFRESH_IP);

    private final LimiteurDeDebit limiteur;
    private final AdresseClient adresseClient;
    private final JsonMapper jsonMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest requete) {
        return !requete.getRequestURI().startsWith(PREFIXE_AUTH)
                || "OPTIONS".equalsIgnoreCase(requete.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest requete,
            @NonNull HttpServletResponse reponse,
            @NonNull FilterChain chaine
    ) throws ServletException, IOException {

        String ip = adresseClient.de(requete);
        try {
            limiteur.consommer(LimiteurDeDebit.AUTH_IP, ip);
            String regle = REGLE_PAR_ROUTE.get(requete.getRequestURI());
            if (regle != null) {
                limiteur.consommer(regle, ip);
            }
        } catch (TropDeRequetesException e) {
            ecrireRefus(reponse, e);
            return;
        }

        chaine.doFilter(requete, reponse);
    }

    private void ecrireRefus(HttpServletResponse reponse, TropDeRequetesException e) throws IOException {
        reponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        reponse.setHeader("Retry-After", Long.toString(e.getSecondesAvantNouvelEssai()));
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(reponse.getWriter(), ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(), "Trop de requetes", e.getMessage()));
    }
}