package com.loupsolitaire.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.loupsolitaire.backend.config.UtilisateurConnecte;
import com.loupsolitaire.backend.exception.ConflitException;
import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.exception.TropDeRequetesException;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.repository.UtilisateurRepository;
import com.loupsolitaire.backend.request.RegisterRequest;
import com.loupsolitaire.backend.request.UpdateProfilRequest;
import com.loupsolitaire.backend.response.AuthResponse;
import com.loupsolitaire.backend.response.PersonnageResponse;
import com.loupsolitaire.backend.response.UtilisateurResponse;
import com.loupsolitaire.backend.service.mapper.PersonnageMapper;

@ExtendWith(MockitoExtension.class)
class CompteServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PersonnageRepository personnageRepository;
    @Mock
    private PersonnageMapper personnageMapper;
    @Mock
    private PersonnageService personnageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthService authService;
    @Mock
    private LimiteurDeDebit limiteurDeDebit;

    @InjectMocks
    private CompteService compteService;

    private final UtilisateurConnecte connecte = new UtilisateurConnecte(UUID.randomUUID());
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(connecte.id());
        utilisateur.setUsername("marius");
        utilisateur.setEmail("marius@example.com");
        utilisateur.setPassword("hash-actuel");
        utilisateur.setEmailVerifie(true);
    }

    private void utilisateurConnecteExiste() {
        when(utilisateurRepository.findById(connecte.id())).thenReturn(Optional.of(utilisateur));
    }

    private RegisterRequest inscription() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("marius");
        request.setEmail("marius@example.com");
        request.setPassword("motdepasse123");
        return request;
    }

    // =========================================================
    // Inscription
    // =========================================================

    @Test
    void inscrireCreeUnCompteAvecUnMotDePasseHacheEtEnvoieLeLienDeVerification() {
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");

        UtilisateurResponse reponse = compteService.inscrire(inscription());

        ArgumentCaptor<Utilisateur> cree = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(cree.capture());
        assertThat(cree.getValue().getPassword()).isEqualTo("hash");
        assertThat(cree.getValue().isEmailVerifie()).isFalse();
        assertThat(cree.getValue().getDateCreation()).isNotNull();
        verify(emailVerificationService).envoyerLienDeVerification(cree.getValue());
        assertThat(reponse.username()).isEqualTo("marius");
    }

    @Test
    void inscrireRefuseUnUsernameDejaPris() {
        when(utilisateurRepository.existsByUsername("marius")).thenReturn(true);

        assertThatThrownBy(() -> compteService.inscrire(inscription()))
                .isInstanceOf(ConflitException.class)
                .hasMessage("Nom d'utilisateur deja utilise");

        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void inscrireRefuseUnEmailDejaPris() {
        when(utilisateurRepository.existsByEmail("marius@example.com")).thenReturn(true);

        assertThatThrownBy(() -> compteService.inscrire(inscription()))
                .isInstanceOf(ConflitException.class)
                .hasMessage("Cet email est deja utilise");

        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }

    // =========================================================
    // Renvoi du lien de verification
    // =========================================================

    @Test
    void renvoyerVerificationEnvoieUnLienSiLeCompteN_estPasVerifie() {
        utilisateur.setEmailVerifie(false);
        when(utilisateurRepository.findByEmail("marius@example.com")).thenReturn(Optional.of(utilisateur));

        compteService.renvoyerVerification("marius@example.com");

        verify(emailVerificationService).envoyerLienDeVerification(utilisateur);
    }

    @Test
    void renvoyerVerificationCompteLaDemandePourL_email() {
        when(utilisateurRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        compteService.renvoyerVerification("inconnu@example.com");

        verify(limiteurDeDebit).consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, "inconnu@example.com");
    }

    @Test
    void renvoyerVerificationNeFaitRienSiLeCompteEstDejaVerifie() {
        when(utilisateurRepository.findByEmail("marius@example.com")).thenReturn(Optional.of(utilisateur));

        compteService.renvoyerVerification("marius@example.com");

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void renvoyerVerificationNeFaitRienSiL_emailEstInconnu() {
        when(utilisateurRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        compteService.renvoyerVerification("inconnu@example.com");

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void renvoyerVerificationRetrouveLeCompteParSonPseudo() {
        utilisateur.setEmailVerifie(false);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        compteService.renvoyerVerification("marius");

        verify(emailVerificationService).envoyerLienDeVerification(utilisateur);
    }

    @Test
    void renvoyerVerificationParPseudoCompteSurL_emailDuCompte() {
        // Pseudo et email partagent le meme compteur : sinon on aurait
        // 3 renvois avec le pseudo + 3 avec l'email pour le meme compte.
        utilisateur.setEmailVerifie(false);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        compteService.renvoyerVerification("marius");

        verify(limiteurDeDebit).consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, "marius@example.com");
    }

    @Test
    void renvoyerVerificationPourUnPseudoInconnuCompteSurLeTexteSaisi() {
        when(utilisateurRepository.findByUsername("fantome")).thenReturn(Optional.empty());

        compteService.renvoyerVerification("fantome");

        verify(limiteurDeDebit).consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, "fantome");
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void renvoyerVerificationRefuseAuQuatriemeRenvoi() {
        doThrow(new TropDeRequetesException(3600))
                .when(limiteurDeDebit).consommer(LimiteurDeDebit.RENVOI_VERIFICATION_EMAIL, "marius@example.com");
        utilisateur.setEmailVerifie(false);
        when(utilisateurRepository.findByUsername("marius")).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> compteService.renvoyerVerification("marius"))
                .isInstanceOf(TropDeRequetesException.class);

        verifyNoInteractions(emailVerificationService);
    }

    // =========================================================
    // Profil
    // =========================================================

    @Test
    void profilInclutLesPersonnagesDeL_utilisateur() {
        Personnage personnage = new Personnage();
        PersonnageResponse reponsePersonnage = new PersonnageResponse(UUID.randomUUID(), "Loup Solitaire",
                15, 15, 0, 20, 20, List.of(), null, 17, null, false, List.of());
        utilisateurConnecteExiste();
        when(personnageRepository.findByUtilisateur(utilisateur)).thenReturn(List.of(personnage));
        when(personnageMapper.versReponse(personnage)).thenReturn(reponsePersonnage);

        UtilisateurResponse reponse = compteService.profil(connecte);

        assertThat(reponse.username()).isEqualTo("marius");
        assertThat(reponse.personnages()).containsExactly(reponsePersonnage);
    }

    @Test
    void profilRenvoie404SiLeCompteN_existePlus() {
        when(utilisateurRepository.findById(connecte.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compteService.profil(connecte))
                .isInstanceOf(RessourceNonTrouveeException.class);
    }

    @Test
    void modifierProfilChangeL_emailEtRedemandeUneVerification() {
        utilisateurConnecteExiste();
        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setEmail("nouveau@example.com");

        UtilisateurResponse reponse = compteService.modifierProfil(connecte, request);

        assertThat(utilisateur.getEmail()).isEqualTo("nouveau@example.com");
        assertThat(utilisateur.isEmailVerifie()).isFalse();
        assertThat(reponse.emailVerifie()).isFalse();
        verify(emailVerificationService).envoyerLienDeVerification(utilisateur);
    }

    @Test
    void modifierProfilNeRedemandePasDeVerificationSiL_emailNeChangeQueDeCasse() {
        utilisateurConnecteExiste();
        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setEmail("MARIUS@example.com");

        compteService.modifierProfil(connecte, request);

        assertThat(utilisateur.isEmailVerifie()).isTrue();
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void modifierProfilRefuseUnEmailDejaUtiliseParUnAutreCompte() {
        utilisateurConnecteExiste();
        when(utilisateurRepository.existsByEmail("prisparunautre@example.com")).thenReturn(true);
        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setEmail("prisparunautre@example.com");

        assertThatThrownBy(() -> compteService.modifierProfil(connecte, request))
                .isInstanceOf(ConflitException.class);

        assertThat(utilisateur.getEmail()).isEqualTo("marius@example.com");
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void modifierProfilRefuseUnUsernameDejaPris() {
        utilisateurConnecteExiste();
        when(utilisateurRepository.existsByUsername("bob")).thenReturn(true);
        UpdateProfilRequest request = new UpdateProfilRequest();
        request.setUsername("bob");

        assertThatThrownBy(() -> compteService.modifierProfil(connecte, request))
                .isInstanceOf(ConflitException.class)
                .hasMessage("Ce nom d'utilisateur est deja utilise");

        assertThat(utilisateur.getUsername()).isEqualTo("marius");
    }

    @Test
    void modifierProfilSansAucunChampNeChangeRien() {
        utilisateurConnecteExiste();

        compteService.modifierProfil(connecte, new UpdateProfilRequest());

        assertThat(utilisateur.getUsername()).isEqualTo("marius");
        assertThat(utilisateur.getEmail()).isEqualTo("marius@example.com");
        verifyNoInteractions(emailVerificationService);
    }

    // =========================================================
    // Mot de passe
    // =========================================================

    @Test
    void changerMotDePasseRevoqueLesSessionsEtEnOuvreUneNouvelle() {
        AuthResponse nouvelleSession = new AuthResponse("access", "refresh");
        utilisateurConnecteExiste();
        when(passwordEncoder.matches("ancien", "hash-actuel")).thenReturn(true);
        when(passwordEncoder.encode("nouveaumotdepasse")).thenReturn("nouveau-hash");
        when(authService.ouvrirSession(utilisateur)).thenReturn(nouvelleSession);

        AuthResponse reponse = compteService.changerMotDePasse(connecte, "ancien", "nouveaumotdepasse");

        assertThat(utilisateur.getPassword()).isEqualTo("nouveau-hash");
        assertThat(reponse).isEqualTo(nouvelleSession);
        // Revocation AVANT la nouvelle session : sinon elle serait revoquee aussi.
        InOrder ordre = inOrder(refreshTokenService, authService);
        ordre.verify(refreshTokenService).revoquerToutesLesSessions(utilisateur);
        ordre.verify(authService).ouvrirSession(utilisateur);
    }

    @Test
    void changerMotDePasseRefuseUnMotDePasseActuelIncorrect() {
        utilisateurConnecteExiste();
        when(passwordEncoder.matches("faux", "hash-actuel")).thenReturn(false);

        assertThatThrownBy(() -> compteService.changerMotDePasse(connecte, "faux", "nouveaumotdepasse"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(utilisateur.getPassword()).isEqualTo("hash-actuel");
        verifyNoInteractions(refreshTokenService, authService);
    }

    // =========================================================
    // Suppression du compte
    // =========================================================

    @Test
    void supprimerCompteSupprimeLesDependancesAvantL_utilisateur() {
        Personnage personnage = new Personnage();
        utilisateurConnecteExiste();
        when(passwordEncoder.matches("motdepasse123", "hash-actuel")).thenReturn(true);
        when(personnageRepository.findByUtilisateur(utilisateur)).thenReturn(List.of(personnage));

        compteService.supprimerCompte(connecte, "motdepasse123");

        InOrder ordre = inOrder(personnageService, refreshTokenService, passwordResetService,
                emailVerificationService, utilisateurRepository);
        ordre.verify(personnageService).supprimerPersonnage(personnage);
        ordre.verify(refreshTokenService).supprimerToutesLesSessions(utilisateur);
        ordre.verify(passwordResetService).supprimerTokens(utilisateur);
        ordre.verify(emailVerificationService).supprimerTokens(utilisateur);
        ordre.verify(utilisateurRepository).delete(utilisateur);
    }

    @Test
    void supprimerCompteRefuseUnMotDePasseIncorrectSansRienSupprimer() {
        utilisateurConnecteExiste();
        when(passwordEncoder.matches("faux", "hash-actuel")).thenReturn(false);

        assertThatThrownBy(() -> compteService.supprimerCompte(connecte, "faux"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(personnageService, refreshTokenService, passwordResetService, emailVerificationService);
        verify(utilisateurRepository, never()).delete(any());
    }
}