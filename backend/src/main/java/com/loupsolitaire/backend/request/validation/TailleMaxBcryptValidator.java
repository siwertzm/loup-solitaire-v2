package com.loupsolitaire.backend.request.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TailleMaxBcryptValidator implements ConstraintValidator<TailleMaxBcrypt, String> {

    // Limite de l'algorithme BCrypt.
    static final int OCTETS_MAX = 72;

    @Override
    public boolean isValid(String valeur, ConstraintValidatorContext context) {
        // null est gere par @NotBlank.
        return valeur == null || valeur.getBytes(StandardCharsets.UTF_8).length <= OCTETS_MAX;
    }
}