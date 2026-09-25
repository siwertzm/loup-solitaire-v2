package com.loupsolitaire.backend.config;

import java.util.Optional;
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
    //
    // Une seule recherche a la fois, jamais "username = X OR email = X" : si un
    // nom d'utilisateur etait egal a l'email d'un autre compte, cette requete
    // renverrait deux lignes (erreur 500, et le vrai proprietaire de l'email ne
    // pourrait plus se connecter). Les nouveaux noms ne peuvent plus contenir
    // "@" (RegisterRequest/UpdateProfilRequest), mais la regle ci-dessous
    // protege aussi les comptes crees avant cette validation :
    // - identifiant avec "@" : l'EMAIL est prioritaire, puis le nom en repli
    //   (pour un ancien compte dont le nom contiendrait "@") ;
    // - identifiant sans "@" : ce ne peut etre qu'un nom d'utilisateur.
    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        Optional<Utilisateur> trouve = identifiant.contains("@")
                ? utilisateurRepository.findByEmail(identifiant)
                        .or(() -> utilisateurRepository.findByUsername(identifiant))
                : utilisateurRepository.findByUsername(identifiant);

        Utilisateur utilisateur = trouve
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