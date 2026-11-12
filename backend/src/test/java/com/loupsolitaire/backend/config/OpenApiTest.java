package com.loupsolitaire.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * QUAL-02 : avec SPRINGDOC_ENABLED=true, la description OpenAPI est generee
 * et decrit les routes de l'API.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Contexte a part (springdoc active) : base H2 a lui et sans cache de
        // second niveau. Ce cache (JCache) est commun a toute la JVM : il
        // contient deja le catalogue charge par les autres contextes de test,
        // et Hibernate n'insererait alors pas les disciplines dans cette base.
        "spring.datasource.url=jdbc:h2:mem:openapi;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "jwt.secret=dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMzJi",
        "jwt.refresh-expiration-days=30",
        "spring.datasource.hikari.connection-init-sql=",
        "springdoc.api-docs.enabled=true"
})
class OpenApiTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void laDescriptionOpenApiDecritLesRoutes() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/version']").exists())
                .andExpect(jsonPath("$.paths['/personnages']").exists());
    }
}
