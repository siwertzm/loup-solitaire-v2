package com.loupsolitaire.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationEchoueeRenvoie400AvecLesErreursParChamp() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objet");
        bindingResult.addError(new FieldError("objet", "nom", "Le nom est obligatoire"));

        // MethodParameter construit sur une vraie methode publique existante
        // (handleValidation lui-meme, qui prend bien un
        // MethodArgumentNotValidException en parametre) plutot que sur une
        // methode bidon locale a ce test : plus robuste vis-a-vis de la
        // reflexion, aucun risque de NoSuchMethodException.
        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandler.class.getDeclaredMethod("handleValidation", MethodArgumentNotValidException.class),
                0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> reponse = handler.handleValidation(ex);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().error()).isEqualTo("Validation echouee");
        assertThat(reponse.getBody().message()).isEqualTo(Map.of("nom", "Le nom est obligatoire"));
    }

    @Test
    void illegalArgumentRenvoie400() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleIllegalArgument(new IllegalArgumentException("valeur invalide"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().message()).isEqualTo("valeur invalide");
    }

    @Test
    void inventairePleinRenvoie409() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleInventairePlein(new InventairePleinException("plus de place dans l'inventaire"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().message()).isEqualTo("plus de place dans l'inventaire");
    }

    @Test
    void conflitExceptionRenvoie409() {
        ResponseEntity<ErrorResponse> reponse = handler.handleConflit(new ConflitException("deja pris"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().message()).isEqualTo("deja pris");
    }

    @Test
    void ressourceNonTrouveeRenvoie404() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleNonTrouvee(new RessourceNonTrouveeException("introuvable"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accesRefuseRenvoie403() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleAccesRefuse(new AccesRefuseException("interdit"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void compteNonVerifieRenvoie403() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleCompteNonVerifie(new CompteNonVerifieException("non verifie"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenInvalideRenvoie401() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleTokenInvalide(new TokenInvalideException("token invalide"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mauvaisIdentifiantsRenvoie401SansDetailPrecis() {
        ResponseEntity<ErrorResponse> reponse =
                handler.handleAuthFailure(new BadCredentialsException("mauvais mot de passe"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Le message ne doit jamais reveler si c'est le login ou le mot de passe qui est faux.
        assertThat(reponse.getBody().message()).isEqualTo("Nom d'utilisateur ou mot de passe incorrect");
    }
}