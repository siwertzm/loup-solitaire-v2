package com.loupsolitaire.backend.config;

import java.util.UUID;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    // Utilise au LOGIN (AuthenticationManager) : l'identifiant saisi peut etre
    // le nom d'utilisateur ou l'email.
    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsernameOrEmail(identifiant, identifiant)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + identifiant));

        return versUserDetails(utilisateur);
    }

    // Utilise par JwtFilter a chaque requete authentifiee : le token porte
    // l'identifiant (UUID) de l'utilisateur, jamais son nom (modifiable).
    // Le UserDetails renvoye contient le nom ACTUEL de l'utilisateur, lu en
    // base : les controleurs peuvent donc continuer a s'appuyer sur
    // userDetails.getUsername(), meme juste apres un changement de nom.
    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + id));

        return versUserDetails(utilisateur);
    }

    private UserDetails versUserDetails(Utilisateur utilisateur) {
        return User.builder()
                .username(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}