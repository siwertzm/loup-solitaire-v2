package com.loupsolitaire.backend.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                erreurs.put(fe.getField(), fe.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation echouee", erreurs));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Requete invalide", ex.getMessage()));
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ErrorResponse> handleConflit(ConflitException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflit", ex.getMessage()));
    }

    @ExceptionHandler(InventairePleinException.class)
    public ResponseEntity<ErrorResponse> handleInventairePlein(InventairePleinException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Inventaire plein", ex.getMessage()));
    }

    @ExceptionHandler(RessourceNonTrouveeException.class)
    public ResponseEntity<ErrorResponse> handleNonTrouvee(RessourceNonTrouveeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "Ressource introuvable", ex.getMessage()));
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<ErrorResponse> handleAccesRefuse(AccesRefuseException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Acces refuse", ex.getMessage()));
    }

    @ExceptionHandler(CompteNonVerifieException.class)
    public ResponseEntity<ErrorResponse> handleCompteNonVerifie(CompteNonVerifieException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Compte non verifie", ex.getMessage()));
    }

    @ExceptionHandler(TokenInvalideException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalide(TokenInvalideException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Session invalide", ex.getMessage()));
    }

    // Limite de debit atteinte (SEC-02, voir LimiteurDeDebit).
    @ExceptionHandler(TropDeRequetesException.class)
    public ResponseEntity<ErrorResponse> handleTropDeRequetes(TropDeRequetesException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getSecondesAvantNouvelEssai()))
                .body(ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(), "Trop de requetes", ex.getMessage()));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthFailure(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Authentification echouee",
                        "Nom d'utilisateur ou mot de passe incorrect"));
    }

    // Deux actions simultanees sur le meme personnage (double tap, double
    // clic, deux onglets...) : la premiere a ete appliquee, celle-ci arrive
    // sur un etat perime et est annulee (voir Personnage.version). Le client
    // peut simplement recharger l'etat du personnage.
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConflitConcurrent(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Action simultanee",
                        "Une autre action vient d'etre appliquee a ce personnage, rechargez puis reessayez"));
    }

    // SEC-08 : contrainte d'unicite violee par deux requetes simultanees
    // (ex. deux inscriptions avec le meme email : les deux passent le
    // existsByEmail, la base refuse la seconde). Le message SQL (tables,
    // contraintes) n'est jamais renvoye.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleViolationUnicite(DataIntegrityViolationException ex) {
        // RGPD-03 : seulement le nom de la contrainte. Le message de
        // PostgreSQL contient la valeur en double ("Key (email)=(...)").
        log.warn("Violation de contrainte en base : {}", nomDeContrainte(ex));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Conflit",
                        "Cette donnee existe deja (nom d'utilisateur ou email)"));
    }

    // SEC-08 : repli pour toute autre exception.
    // - Erreurs de Spring MVC (route inconnue 404, methode 405, JSON
    //   illisible 400...) : elles portent deja leur statut, on le garde.
    // - Le reste est une vraie erreur interne : envoyee a Sentry (son
    //   resolveur passe APRES ce gestionnaire, sentry.exception-resolver-order=1,
    //   il ne la verrait plus), journalisee avec une reference, et renvoyee
    //   sans aucun detail interne.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErreurInattendue(Exception ex) throws Exception {
        // Refus de Spring Security (401/403) : laisses a son ExceptionTranslationFilter.
        if (ex instanceof AuthenticationException || ex instanceof AccessDeniedException) {
            throw ex;
        }
        // Corps JSON illisible, parametre du mauvais type (ex. UUID invalide
        // dans l'URL) : 400, comme le fait Spring sans ce gestionnaire.
        if (ex instanceof HttpMessageNotReadableException || ex instanceof TypeMismatchException) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Requete invalide",
                            "Requete illisible ou parametre invalide"));
        }
        if (ex instanceof org.springframework.web.ErrorResponse erreurSpring) {
            int statut = erreurSpring.getStatusCode().value();
            HttpStatus connu = HttpStatus.resolve(statut);
            return ResponseEntity.status(statut)
                    .body(ErrorResponse.of(statut, connu != null ? connu.getReasonPhrase() : "Erreur",
                            erreurSpring.getBody().getDetail()));
        }

        SentryId evenement = Sentry.captureException(ex);
        String reference = SentryId.EMPTY_ID.equals(evenement)
                ? UUID.randomUUID().toString().substring(0, 8)
                : evenement.toString();
        log.error("Erreur interne, reference {}", reference, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne",
                        "Une erreur est survenue (reference " + reference + ")"));
    }

    private static String nomDeContrainte(Throwable erreur) {
        for (Throwable cause = erreur; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return "inconnue";
    }
}
