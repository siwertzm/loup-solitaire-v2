package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(
            regexp = "\\d{6}",
            message = "Le code doit contenir 6 chiffres"
    )
    private String code;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(
            min = 8,
            message = "Le mot de passe doit contenir au moins 8 caracteres"
    )
    private String newPassword;
}