package com.loupsolitaire.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * SEC-04 : revocation de toutes les sessions d'un utilisateur, dans une
 * transaction SEPAREE qui est validee tout de suite.
 *
 * Cas d'usage : un refresh token deja utilise est presente (vol ou rejeu).
 * RefreshTokenService revoque alors toutes les sessions, puis leve une
 * exception pour refuser la requete. Dans la transaction de la requete, cette
 * exception annulait aussi la revocation : l'attaquant gardait sa session.
 * Ici, REQUIRES_NEW valide la revocation avant que l'exception ne remonte,
 * quel que soit l'appelant (AuthService.rafraichir est lui aussi
 * transactionnel).
 *
 * Classe a part (et non une methode de RefreshTokenService) : un appel
 * interne a la meme classe ne passe pas par le proxy Spring et ignorerait
 * REQUIRES_NEW.
 */
@Service
@RequiredArgsConstructor
public class RevocationDesSessions {

    private final RefreshTokenRepository refreshTokenRepository;

    /** @return le nombre de sessions actives revoquees */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revoquerToutesImmediatement(UUID utilisateurId) {
        return refreshTokenRepository.revoquerToutesLesSessionsActives(utilisateurId);
    }
}