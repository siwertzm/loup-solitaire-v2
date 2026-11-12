package com.loupsolitaire.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loupsolitaire.backend.model.RefreshToken;
import com.loupsolitaire.backend.model.Utilisateur;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUtilisateurAndRevokedFalse(Utilisateur utilisateur);

    // SEC-04 : une seule requete UPDATE, sans charger les entites. Utilisee
    // par RevocationDesSessions, dans sa propre transaction.
    @Modifying
    @Query("update RefreshToken t set t.revoked = true "
            + "where t.utilisateur.id = :utilisateurId and t.revoked = false")
    int revoquerToutesLesSessionsActives(@Param("utilisateurId") UUID utilisateurId);

    // Necessaire avant UtilisateurRepository.delete(utilisateur) : sans ca, la
    // contrainte de cle etrangere utilisateur_id bloque la suppression du
    // compte (voir AuthController.deleteAccount).
    void deleteByUtilisateur(Utilisateur utilisateur);

    // OPS-05 : sessions expirees avant cette date (les revoquees non
    // expirees restent : elles servent a detecter la reutilisation, SEC-04).
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :limite")
    int supprimerExpiresAvant(@Param("limite") Instant limite);
}
