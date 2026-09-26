package com.loupsolitaire.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.JwtUtil;
import com.loupsolitaire.backend.exception.CompteNonVerifieException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.response.AuthResponse;

import lombok.RequiredArgsConstructor;

/**
 * Sessions : connexion, rafraichissement et ouverture d'une nouvelle session
 * (couple access token + refresh token).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LimiteurDeDebit limiteurDeDebit;

    // identifiant accepte nom d'utilisateur OU email : voir
    // CustomUserDetailsService. Mauvais identifiants : BadCredentialsException
    // (401, voir GlobalExceptionHandler).
    //
    // SEC-02 : au-dela de N mots de passe faux pour un meme identifiant dans
    // la fenetre, 429 sans meme verifier le mot de passe (force brute
    // repartie sur plusieurs IP). Une connexion reussie remet le compteur a
    // zero. Le compteur existe aussi pour un identifiant inconnu : la reponse
    // ne revele donc pas si le compte existe.
    @Transactional
    public AuthResponse connecter(String identifiant, String motDePasse) {
        limiteurDeDebit.verifier(LimiteurDeDebit.LOGIN_COMPTE, identifiant);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifiant, motDePasse));
        } catch (AuthenticationException e) {
            limiteurDeDebit.enregistrerEchec(LimiteurDeDebit.LOGIN_COMPTE, identifiant);
            throw e;
        }
        limiteurDeDebit.reinitialiser(LimiteurDeDebit.LOGIN_COMPTE, identifiant);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));

        if (!utilisateur.isEmailVerifie()) {
            throw new CompteNonVerifieException(
                    "Merci de confirmer ton adresse email avant de te connecter (verifie ta boite mail)");
        }

        return ouvrirSession(utilisateur);
    }

    // Echange un refresh token valide contre un nouveau couple de tokens (le
    // refresh token presente est revoque : rotation, voir RefreshTokenService).
    // Ne necessite PAS d'access token valide : c'est justement le cas d'usage.
    @Transactional
    public AuthResponse rafraichir(String refreshToken) {
        RefreshTokenService.RotationResult resultat = refreshTokenService.validerEtPivoter(refreshToken);
        return new AuthResponse(jwtUtil.generateToken(resultat.utilisateurId()), resultat.nouveauRefreshToken());
    }

    // Nouveau couple de tokens pour cet utilisateur (connexion, changement de
    // mot de passe).
    @Transactional
    public AuthResponse ouvrirSession(Utilisateur utilisateur) {
        String accessToken = jwtUtil.generateToken(utilisateur.getId());
        String refreshToken = refreshTokenService.creerToken(utilisateur);
        return new AuthResponse(accessToken, refreshToken);
    }

    // Deconnexion : revoque le refresh token de cet appareil.
    @Transactional
    public void deconnecter(String refreshToken) {
        refreshTokenService.revoquer(refreshToken);
    }
}