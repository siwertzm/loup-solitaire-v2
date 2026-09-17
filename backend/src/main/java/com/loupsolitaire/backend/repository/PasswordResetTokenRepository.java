package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.PasswordResetToken;
import com.loupsolitaire.backend.model.Utilisateur;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken>
            findTopByUtilisateurAndUtiliseFalseOrderByCreatedAtDesc(
                    Utilisateur utilisateur
            );

    List<PasswordResetToken> findByUtilisateurAndUtiliseFalse(
            Utilisateur utilisateur
    );
}