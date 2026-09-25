package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caracteres")
    // Interdit "@" : le login accepte le nom d'utilisateur OU l'email. Un nom
    // contenant "@" pourrait reprendre l'email d'un autre joueur et rendre
    // la connexion ambigue (voir CustomUserDetailsService).
    @Pattern(regexp = "^[^@]*$", message = "Le nom d'utilisateur ne peut pas contenir le caractere @")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String password;

    // Optionnelle : peut etre completee plus tard via PUT /auth/me.
    @Past(message = "La date de naissance doit etre dans le passe")
    private LocalDate dateNaissance;
}