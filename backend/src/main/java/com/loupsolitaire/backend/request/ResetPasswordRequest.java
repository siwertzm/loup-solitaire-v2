package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Le token de reinitialisation est obligatoire")
    private String resetToken;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(
            min = 8,
            message = "Le mot de passe doit contenir au moins 8 caracteres"
    )
    private String newPassword;
}