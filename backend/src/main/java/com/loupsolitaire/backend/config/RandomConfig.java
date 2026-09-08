package com.loupsolitaire.backend.config;

import java.util.Random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Bean injectable plutot que "new Random()" en dur dans les services : permet
// de mocker le tirage dans les tests (predictibilite des tirages 0-9).
@Configuration
public class RandomConfig {

    @Bean
    public Random random() {
        return new Random();
    }
}