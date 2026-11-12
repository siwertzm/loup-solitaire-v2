package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.exception.TropDeRequetesException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;
import com.loupsolitaire.backend.util.Emails;

import lombok.RequiredArgsConstructor;

/**
 * Compte utilisateur : inscription, verification de l'email, profil, mot de
 * passe et suppression.
 *
 * L'utilisateur est charge dans la transaction de chaque methode : ses
 * modifications sont enregistrees automatiquement a la validation (seule la
 * creation d'un compte utilise save()).
 */
@Service
@RequiredArgsConstructor
public class CompteService {

    private final UtilisateurRepository utilisateurRepository;
    private final PersonnageRepository personnageRepository;
    private final PersonnageMapper personnageMapper;
    private final PersonnageService personnageService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final LimiteurDeDebit limiteurDeDebit;
    private final EmailService emailService;

    // SEC-05 : la reponse ne dit jamais si l'email a deja un compte (le
    // controleur repond toujours 202 sans contenu). Si c'est le cas, le
    // compte n'est pas cree et un email part a cette adresse. Le nom
    // d'utilisateur pris reste un 409 : le joueur doit en choisir un autre,
    // et ce nom ne revele pas l'email.
    @Transactional
    public void inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new ConflitException("Nom d'utilisateur deja utilise");
        }

        // Hache dans tous les cas : sans ca, la duree de la reponse (bcrypt,
        // environ 100 ms) suffirait a savoir si l'email existe.
        String motDePasseHache = passwordEncoder.encode(request.getPassword());

        Optional<Utilisateur> existant = utilisateurRepository.findByEmail(request.getEmail());
        if (existant.isPresent()) {
            prevenirCompteExistant(existant.get());
            return;
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(request.getUsername());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setPassword(motDePasseHache);
        utilisateur.setDateCreation(Instant.now());
        utilisateurRepository.save(utilisateur);

        emailVerificationService.envoyerLienDeVerification(utilisateur);
    }

    // SEC-05 : quelqu'un s'inscrit avec un email deja enregistre. Compte non
    // verifie : le lien est renvoye ; sinon, rappel "tu as deja un compte".
    // Au plus 3 par heure et par adresse (meme compteur que le renvoi du
    // lien) ; au-dela, rien ne part, sans erreur : la reponse doit rester
    // identique a celle d'une inscription normale.
    private void prevenirCompteExistant(Utilisateur utilisateur) {
        try {
            limiteurDeDebit.consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, utilisateur.getEmail());
        } catch (TropDeRequetesException e) {
            return;
        }
        if (utilisateur.isEmailVerifie()) {
            emailService.envoyerRappelCompteExistant(utilisateur.getEmail(), utilisateur.getUsername());
        } else {
            emailVerificationService.envoyerLienDeVerification(utilisateur);
        }
    }

    // Ne dit jamais si le compte existe ou s'il est deja verifie : le
    // controleur repond toujours la meme chose, pour ne pas laisser deviner
    // quels comptes sont enregistres.
    //
    // identifiant : email ou pseudo (ecran de connexion). Le lien part
    // toujours a l'email enregistre sur le compte, jamais a une adresse
    // fournie par l'appelant.
    @Transactional
    public void renvoyerVerification(String identifiant) {
        Optional<Utilisateur> compte = trouverParIdentifiant(identifiant);

        // SEC-02 : 3 renvois par heure et par compte. La cle est l'email du
        // compte, pour que pseudo et email partagent le meme compteur ; pour
        // un identifiant inconnu, c'est le texte saisi (pas d'enumeration).
        String cle = compte.map(Utilisateur::getEmail).orElse(identifiant);
        limiteurDeDebit.consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, cle);

        // SEC-06 : aussi pour un compte verifie qui attend la confirmation
        // d'un nouvel email (le lien part alors a la nouvelle adresse).
        compte.filter(utilisateur -> !utilisateur.isEmailVerifie() || utilisateur.getEmailEnAttente() != null)
                .ifPresent(emailVerificationService::envoyerLienDeVerification);
    }

    // Meme resolution que la connexion (CustomUserDetailsService) : un
    // identifiant avec @ est d'abord cherche comme email.
    private Optional<Utilisateur> trouverParIdentifiant(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            return Optional.empty();
        }
        String saisi = identifiant.trim();
        return saisi.contains("@")
                ? utilisateurRepository.findByEmail(Emails.normaliser(saisi))
                        .or(() -> utilisateurRepository.findByUsername(saisi))
                : utilisateurRepository.findByUsername(saisi);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse profil(UtilisateurConnecte connecte) {
        Utilisateur utilisateur = recupererUtilisateur(connecte);

        List<PersonnageResponse> personnages = personnageRepository.findByUtilisateur(utilisateur).stream()
                .map(personnageMapper::versReponse)
                .toList();

        return UtilisateurResponse.fromEntity(utilisateur, personnages);
    }

    // Champs optionnels : seuls ceux fournis (non null) sont mis a jour.
    @Transactional
    public UtilisateurResponse modifierProfil(UtilisateurConnecte connecte, UpdateProfilRequest request) {
        Utilisateur utilisateur = recupererUtilisateur(connecte);

        if (request.getUsername() != null && !request.getUsername().equals(utilisateur.getUsername())) {
            if (utilisateurRepository.existsByUsername(request.getUsername())) {
                throw new ConflitException("Ce nom d'utilisateur est deja utilise");
            }
            utilisateur.setUsername(request.getUsername());
        }

        // SEC-06 : changer d'email exige le mot de passe (un telephone
        // deverrouille ou un access token vole ne suffit plus). Le nouvel
        // email reste en attente : l'ancien sert a se connecter jusqu'a la
        // confirmation (voir EmailVerificationService).
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(utilisateur.getEmail())) {
            if (request.getMotDePasseActuel() == null
                    || !passwordEncoder.matches(request.getMotDePasseActuel(), utilisateur.getPassword())) {
                throw new BadCredentialsException("Mot de passe actuel incorrect");
            }
            if (utilisateurRepository.existsByEmail(request.getEmail())) {
                throw new ConflitException("Cet email est deja utilise");
            }
            emailVerificationService.demanderChangementEmail(utilisateur, request.getEmail());
        }

        return UtilisateurResponse.fromEntity(utilisateur);
    }

    // Verifie le mot de passe actuel avant d'appliquer le nouveau. Par
    // securite, toutes les sessions sont revoquees : seule la session
    // courante repart avec un nouveau couple de tokens.
    @Transactional
    public AuthResponse changerMotDePasse(UtilisateurConnecte connecte, String motDePasseActuel, String nouveauMotDePasse) {
        Utilisateur utilisateur = recupererUtilisateur(connecte);

        if (!passwordEncoder.matches(motDePasseActuel, utilisateur.getPassword())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect");
        }

        utilisateur.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        refreshTokenService.revoquerToutesLesSessions(utilisateur);

        return authService.ouvrirSession(utilisateur);
    }

    // Suppression definitive et irreversible, apres verification du mot de
    // passe. Supprime d'abord tout ce qui depend de l'utilisateur
    // (personnages et leurs donnees, puis tokens de session, de verification
    // et de reinitialisation), sinon les cles etrangeres bloqueraient la
    // suppression de l'utilisateur lui-meme.
    @Transactional
    public void supprimerCompte(UtilisateurConnecte connecte, String motDePasse) {
        Utilisateur utilisateur = recupererUtilisateur(connecte);

        if (!passwordEncoder.matches(motDePasse, utilisateur.getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }

        personnageRepository.findByUtilisateur(utilisateur)
                .forEach(personnageService::supprimerPersonnage);

        refreshTokenService.supprimerToutesLesSessions(utilisateur);
        passwordResetService.supprimerTokens(utilisateur);
        emailVerificationService.supprimerTokens(utilisateur);

        utilisateurRepository.delete(utilisateur);
    }

    private Utilisateur recupererUtilisateur(UtilisateurConnecte connecte) {
        return utilisateurRepository.findById(connecte.id())
                .orElseThrow(() -> new RessourceNonTrouveeException("Utilisateur non trouve"));
    }
}