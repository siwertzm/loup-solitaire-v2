package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.EmailVerificationToken;
import com.loupsolitaire.backend.model.Utilisateur;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    // Necessaire avant UtilisateurRepository.delete(utilisateur) : sans ca, la
    // contrainte de cle etrangere utilisateur_id bloque la suppression du
    // compte (voir AuthController.deleteAccount).
    void deleteByUtilisateur(Utilisateur utilisateur);
}