package com.loupsolitaire.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loupsolitaire.backend.config.JwtUtil;
import com.loupsolitaire.backend.exception.CompteNonVerifieException;
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.AuthRequest;
import com.loupsolitaire.backend.request.RefreshRequest;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.ResendVerificationRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.EmailVerificationService;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;
import com.loupsolitaire.backend.service.RefreshTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PersonnageRepository personnageRepository;
    private final PersonnageMapper personnageMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<UtilisateurResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new ConflitException("Nom d'utilisateur deja utilise");
        }
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new ConflitException("Cet email est deja utilise");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setPassword(passwordEncoder.encode(request.getPassword()));
        utilisateur.setDateNaissance(request.getDateNaissance());
        utilisateurRepository.save(utilisateur);

        emailVerificationService.envoyerLienDeVerification(utilisateur);

        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.fromEntity(utilisateur));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        // identifiant accepte username OU email : voir CustomUserDetailsService.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifiant(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));

        if (!utilisateur.isEmailVerifie()) {
            throw new CompteNonVerifieException(
                    "Merci de confirmer ton adresse email avant de te connecter (verifie ta boite mail)"
            );
        }

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = refreshTokenService.creerToken(utilisateur);

        return new AuthResponse(accessToken, refreshToken);
    }

    // Ouvert depuis un email (clic sur le lien) : reponse HTML simple, pas de JSON.
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifier(token);
        return ResponseEntity.ok(
                "<html><body style=\"font-family:sans-serif;text-align:center;padding:40px\">" +
                "<h2>Email confirme !</h2>" +
                "<p>Ton compte est active, tu peux retourner sur l'application et te connecter.</p>" +
                "</body></html>"
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        utilisateurRepository.findByEmail(request.getEmail()).ifPresent(utilisateur -> {
            if (!utilisateur.isEmailVerifie()) {
                emailVerificationService.envoyerLienDeVerification(utilisateur);
            }
        });
        // Reponse identique que l'email existe ou non, et qu'il soit deja verifie
        // ou non : evite de laisser deviner quels emails sont enregistres.
        return ResponseEntity.accepted().build();
    }

    // Ne necessite PAS de jeton d'acces valide dans l'en-tete Authorization :
    // c'est justement le cas d'usage (le jeton d'acces a expire, on en redemande
    // un via le refresh token, plus longue duree de vie).
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.RotationResult resultat = refreshTokenService.validerEtPivoter(request.getRefreshToken());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(resultat.username())
                .password(resultat.passwordHash())
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

        List<PersonnageResponse> personnages = personnageRepository.findByUtilisateur(utilisateur).stream()
                .map(personnageMapper::versReponse)
                .toList();

        return UtilisateurResponse.fromEntity(utilisateur, personnages);
    }

    // Complete/modifie le profil (email, date de naissance) apres inscription.
    // Champs optionnels : seuls ceux fournis (non null) sont mis a jour.
    @PutMapping("/me")
    public UtilisateurResponse updateProfil(
            @Valid @RequestBody UpdateProfilRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(utilisateur.getEmail())) {
            if (utilisateurRepository.existsByEmail(request.getEmail())) {
                throw new ConflitException("Cet email est deja utilise");
            }
            utilisateur.setEmail(request.getEmail());
            // Changer d'email revoque la verification : il faut reconfirmer la nouvelle adresse.
            utilisateur.setEmailVerifie(false);
            emailVerificationService.envoyerLienDeVerification(utilisateur);
        }

        if (request.getDateNaissance() != null) {
            utilisateur.setDateNaissance(request.getDateNaissance());
        }

        utilisateurRepository.save(utilisateur);
        return UtilisateurResponse.fromEntity(utilisateur);
    }
}