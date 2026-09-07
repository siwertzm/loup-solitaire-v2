package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void chargeUnUtilisateurExistantParUsernameOuEmail() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setPassword("hash");

        when(utilisateurRepository.findByUsernameOrEmail("marius", "marius"))
                .thenReturn(Optional.of(utilisateur));

        UserDetails result = service.loadUserByUsername("marius");

        assertThat(result.getUsername()).isEqualTo("marius");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void leveUneExceptionSiAucunUtilisateurNeCorrespond() {
        when(utilisateurRepository.findByUsernameOrEmail("inconnu", "inconnu"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}