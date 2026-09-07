package com.loupsolitaire.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.JwtUtil;
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.AuthRequest;
import com.loupsolitaire.backend.request.RefreshRequest;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.RefreshTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<UtilisateurResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new ConflitException("Nom d'utilisateur deja utilise");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateurRepository.save(utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.fromEntity(utilisateur));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.creerToken(utilisateur);

        return new AuthResponse(accessToken, refreshToken);
    }

    // Ne necessite PAS de jeton d'acces valide dans l'en-tete Authorization :
    // c'est justement le cas d'usage (le jeton d'acces a expire, on en redemande
    // un via le refresh token, plus longue duree de vie).
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.RotationResult resultat = refreshTokenService.validerEtPivoter(request.getRefreshToken());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(resultat.utilisateur().getUsername())
                .password(resultat.utilisateur().getPassword())
                .authorities("ROLE_USER")
                .build();

        String nouvelAccessToken = jwtUtil.generateToken(userDetails);

        return new AuthResponse(nouvelAccessToken, resultat.nouveauRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoquer(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UtilisateurResponse getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));

        return UtilisateurResponse.fromEntity(utilisateur);
    }
}
