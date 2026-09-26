package com.loupsolitaire.backend.config;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LimitationDebitFilter limitationDebitFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AppProperties appProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        // Necessaire pour le healthcheck Docker (voir docker-compose.yml) :
                        // le conteneur doit pouvoir interroger ce endpoint sans jeton.
                        .requestMatchers("/actuator/health").permitAll()
                        // RGPD-04 : politique de confidentialite et CGU, pages
                        // statiques publiques (liens depuis l'app, les emails et
                        // les stores).
                        .requestMatchers("/legal/**").permitAll()
                        // OPS-04 : 404 tant que app.diagnostic.erreur-test=false.
                        .requestMatchers("/diagnostic/erreur-test").permitAll()
                        // Page d'erreur de Spring Boot : sans cette regle, une erreur
                        // 500 est renvoyee vers /error, que la securite bloque, et le
                        // client recoit un 401 "jeton manquant" au lieu de la 500.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated()
                )
                // Limitation de debit d'abord (SEC-02) : une requete refusee ne
                // coute ni verification de JWT, ni BCrypt, ni envoi d'email.
                .addFilterBefore(limitationDebitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Origines lues depuis app.cors.allowed-origins (variable d'environnement
    // CORS_ALLOWED_ORIGINS). Unique source de verite pour le CORS : evite
    // l'incoherence @CrossOrigin/SecurityConfig de la V1.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(appProperties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Pas de "credentials" : l'API n'utilise aucun cookie, le token JWT
        // passe dans l'en-tete Authorization (autorise juste au-dessus). Les
        // autoriser quand meme permettrait inutilement aux origines listees
        // d'envoyer des cookies du navigateur.
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}