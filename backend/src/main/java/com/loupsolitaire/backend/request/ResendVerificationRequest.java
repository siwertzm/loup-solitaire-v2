package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.loupsolitaire.backend.util.Emails;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    // Email normalise des la lecture du JSON (espaces retires, minuscules) :
    // toutes les recherches en base se font ensuite sur cette forme unique.
    public void setEmail(String email) {
        this.email = Emails.normaliser(email);
    }
}