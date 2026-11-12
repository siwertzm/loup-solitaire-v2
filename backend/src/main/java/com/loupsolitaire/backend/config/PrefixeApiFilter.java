package com.loupsolitaire.backend.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * QUAL-02 : l'API repond sous /api/v1 ET sous les anciens chemins (sans
 * prefixe), le temps que les apps deja installees se mettent a jour.
 *
 * Le filtre passe en premier (avant Spring Security, la limitation de debit et
 * les controleurs) et retire le prefixe : tout le reste de l'application ne
 * voit que les chemins historiques, une seule regle de securite par route.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrefixeApiFilter extends OncePerRequestFilter {

    static final String PREFIXE = "/api/v1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String contexte = request.getContextPath();
        String chemin = request.getRequestURI().substring(contexte.length());

        if (chemin.equals(PREFIXE) || chemin.startsWith(PREFIXE + "/")) {
            String sansPrefixe = chemin.substring(PREFIXE.length());
            chain.doFilter(new RequeteSansPrefixe(request, sansPrefixe.isEmpty() ? "/" : sansPrefixe), response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static final class RequeteSansPrefixe extends HttpServletRequestWrapper {

        private final String chemin;

        RequeteSansPrefixe(HttpServletRequest request, String chemin) {
            super(request);
            this.chemin = chemin;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + chemin;
        }

        @Override
        public String getServletPath() {
            return chemin;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            boolean portParDefaut = ("http".equals(getScheme()) && getServerPort() == 80)
                    || ("https".equals(getScheme()) && getServerPort() == 443);
            if (!portParDefaut) {
                url.append(':').append(getServerPort());
            }
            return url.append(getRequestURI());
        }
    }
}
