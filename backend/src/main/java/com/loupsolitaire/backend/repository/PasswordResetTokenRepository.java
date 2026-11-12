package com.loupsolitaire.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<PasswordResetToken> findByResetTokenHashAndUtiliseFalse(
            String resetTokenHash
    );

    // Necessaire avant UtilisateurRepository.delete(utilisateur) : sans ca, la
    // contrainte de cle etrangere utilisateur_id bloque la suppression du
    // compte (voir AuthController.deleteAccount).
    void deleteByUtilisateur(Utilisateur utilisateur);

    // OPS-05 : demandes "mot de passe oublie" dont le code ET l'etape de
    // reinitialisation (si elle a ete ouverte) ont expire.
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :maintenant "
            + "and (t.resetTokenExpiresAt is null or t.resetTokenExpiresAt < :maintenant)")
    int supprimerExpires(@Param("maintenant") Instant maintenant);
}
