package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByUsername(String username);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Permet le login via username OU email : les deux arguments recoivent la
    // meme valeur saisie par l'utilisateur (voir AuthController.login).
    Optional<Utilisateur> findByUsernameOrEmail(String username, String email);
}
