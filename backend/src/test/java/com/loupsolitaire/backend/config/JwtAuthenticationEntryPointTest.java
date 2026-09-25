package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JwtAuthenticationEntryPointTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(jsonMapper);

    @Test
    void repondUn401EnJsonPourQueLeClientTenteUnRefresh() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("GET", "/personnages"), response,
                new InsufficientAuthenticationException("Full authentication is required"));

        // 401 et non 403 : l'intercepteur du frontend ne rafraichit le token
        // que sur un 401.
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");

        JsonNode corps = jsonMapper.readTree(response.getContentAsString());
        assertThat(corps.get("status").asInt()).isEqualTo(401);
        assertThat(corps.get("error").asString()).isEqualTo("Non authentifie");
        assertThat(corps.get("message").asString()).isEqualTo("Jeton d'acces manquant, invalide ou expire");
        assertThat(corps.has("timestamp")).isTrue();
    }
}