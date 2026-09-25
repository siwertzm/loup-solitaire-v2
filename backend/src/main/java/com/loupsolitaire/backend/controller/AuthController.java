package com.loupsolitaire.backend.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.AppProperties;
import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.request.AuthRequest;
import com.loupsolitaire.backend.request.ChangePasswordRequest;
import com.loupsolitaire.backend.request.DeleteAccountRequest;
import com.loupsolitaire.backend.request.ForgotPasswordRequest;
import com.loupsolitaire.backend.request.RefreshRequest;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.ResendVerificationRequest;
import com.loupsolitaire.backend.request.ResetPasswordRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.request.VerifyResetCodeRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.ResetCodeResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.AuthService;
import com.loupsolitaire.backend.service.CompteService;
import com.loupsolitaire.backend.service.EmailVerificationService;
import com.loupsolitaire.backend.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Couche HTTP uniquement : les regles sont dans AuthService (sessions),
// CompteService (compte), EmailVerificationService et PasswordResetService.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CompteService compteService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final AppProperties appProperties;

    // =========================================================
    // Inscription et verification de l'email
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<UtilisateurResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.inscrire(request));
    }

    // Lien recu par email : valide le compte puis redirige vers l'application
    // mobile (deep link).
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifier(token);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(appProperties.mobileLoginUrl())).build();
    }

    // Reponse identique que l'email existe ou non, et qu'il soit deja verifie
    // ou non : evite de laisser deviner quels emails sont enregistres.
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        compteService.renvoyerVerification(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    // =========================================================
    // Sessions
    // =========================================================

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.connecter(request.getIdentifiant(), request.getPassword());
    }

    // Ne necessite PAS d'access token valide : c'est justement le cas
    // d'usage (l'access token a expire, on en redemande un).
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.rafraichir(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.deconnecter(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // Mot de passe oublie
    // =========================================================

    // Toujours la meme reponse, que l'adresse existe ou non.
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.demanderReinitialisation(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-reset-code")
    public ResetCodeResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return new ResetCodeResponse(passwordResetService.verifierCode(request.getEmail(), request.getCode()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reinitialiser(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // Compte de l'utilisateur connecte
    // =========================================================

    @GetMapping("/me")
    public UtilisateurResponse getCurrentUser(@AuthenticationPrincipal UtilisateurConnecte connecte) {
        return compteService.profil(connecte);
    }

    // Champs optionnels : seuls ceux fournis (non null) sont mis a jour.
    @PutMapping("/me")
    public UtilisateurResponse updateProfil(
            @Valid @RequestBody UpdateProfilRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {
        return compteService.modifierProfil(connecte, request);
    }

    @PutMapping("/me/password")
    public AuthResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {
        return compteService.changerMotDePasse(connecte, request.getCurrentPassword(), request.getNewPassword());
    }

    // Suppression definitive et irreversible, apres verification du mot de passe.
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            @AuthenticationPrincipal UtilisateurConnecte connecte) {
        compteService.supprimerCompte(connecte, request.getPassword());
    }
}