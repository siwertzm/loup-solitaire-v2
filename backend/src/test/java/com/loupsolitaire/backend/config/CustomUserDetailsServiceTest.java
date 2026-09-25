package com.loupsolitaire.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

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

    private Utilisateur utilisateur(String username, String email) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(username);
        utilisateur.setEmail(email);
        utilisateur.setPassword("hash");
        return utilisateur;
    }

    @Test
    void chargeUnUtilisateurParSonNom() {
        when(utilisateurRepository.findByUsername("marius"))
                .thenReturn(Optional.of(utilisateur("marius", "marius@example.com")));

        UserDetails result = service.loadUserByUsername("marius");

        assertThat(result.getUsername()).isEqualTo("marius");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        // Sans "@", l'identifiant ne peut etre qu'un nom : l'email n'est jamais cherche.
        verify(utilisateurRepository, never()).findByEmail(anyString());
    }

    @Test
    void chargeUnUtilisateurParSonEmail() {
        when(utilisateurRepository.findByEmail("marius@example.com"))
                .thenReturn(Optional.of(utilisateur("marius", "marius@example.com")));

        UserDetails result = service.loadUserByUsername("marius@example.com");

        assertThat(result.getUsername()).isEqualTo("marius");
        verify(utilisateurRepository, never()).findByUsername(anyString());
    }

    @Test
    void retrouveLeCompteQuelleQueSoitLaCasseDeLEmailSaisi() {
        // Les emails sont stockes en minuscules : la saisie est normalisee
        // avant la recherche.
        when(utilisateurRepository.findByEmail("marius@example.com"))
                .thenReturn(Optional.of(utilisateur("marius", "marius@example.com")));

        UserDetails result = service.loadUserByUsername("  Marius@Example.COM ");

        assertThat(result.getUsername()).isEqualTo("marius");
    }

    @Test
    void lEmailEstPrioritaireSurUnNomIdentique() {
        // Cas d'attaque : "pirate" a pris comme nom d'utilisateur l'email de
        // "victime". Se connecter avec cet email doit designer la victime,
        // sans ambiguite ni erreur.
        when(utilisateurRepository.findByEmail("victime@example.com"))
                .thenReturn(Optional.of(utilisateur("victime", "victime@example.com")));

        UserDetails result = service.loadUserByUsername("victime@example.com");

        assertThat(result.getUsername()).isEqualTo("victime");
        verify(utilisateurRepository, never()).findByUsername(anyString());
    }

    @Test
    void unAncienNomContenantUnArobaseResteUtilisablePourSeConnecter() {
        // Compte cree avant l'interdiction du "@" dans les noms, et dont le
        // nom ne correspond a aucun email : repli sur la recherche par nom.
        when(utilisateurRepository.findByEmail("ancien@pseudo")).thenReturn(Optional.empty());
        when(utilisateurRepository.findByUsername("ancien@pseudo"))
                .thenReturn(Optional.of(utilisateur("ancien@pseudo", "ancien@example.com")));

        UserDetails result = service.loadUserByUsername("ancien@pseudo");

        assertThat(result.getUsername()).isEqualTo("ancien@pseudo");
    }

    @Test
    void leveUneExceptionSiAucunUtilisateurNeCorrespond() {
        when(utilisateurRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void leveUneExceptionSiAucunUtilisateurNeCorrespondAUnEmail() {
        when(utilisateurRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());
        when(utilisateurRepository.findByUsername("inconnu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void chargeUnUtilisateurParSonIdentifiantAvecSonNomActuel() {
        UUID id = UUID.randomUUID();
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setUsername("bob2");
        utilisateur.setPassword("hash");

        when(utilisateurRepository.findById(id)).thenReturn(Optional.of(utilisateur));

        UserDetails result = service.loadUserById(id);

        assertThat(result.getUsername()).isEqualTo("bob2");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void leveUneExceptionSiAucunUtilisateurNePorteCetIdentifiant() {
        UUID id = UUID.randomUUID();
        when(utilisateurRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(id))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}