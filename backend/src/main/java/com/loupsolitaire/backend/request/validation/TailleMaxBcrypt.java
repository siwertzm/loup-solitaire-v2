package com.loupsolitaire.backend.request.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Nouveau mot de passe d'au plus 72 OCTETS (UTF-8).
 *
 * BCrypt ne prend en compte que les 72 premiers octets, et
 * BCryptPasswordEncoder refuse de hacher un mot de passe plus long
 * (IllegalArgumentException, qui donnait une erreur peu claire). On le refuse
 * donc des la validation, avec un message lisible.
 *
 * On compte les octets et non les caracteres (@Size) : une lettre accentuee
 * occupe 2 octets, un emoji 4.
 */
@Documented
@Constraint(validatedBy = TailleMaxBcryptValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TailleMaxBcrypt {

    String message() default "Le mot de passe est trop long (72 caracteres maximum, les accents comptent double)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}