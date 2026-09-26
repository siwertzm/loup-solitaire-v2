package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Email;
import com.loupsolitaire.backend.request.validation.TailleMaxBcrypt;
import com.loupsolitaire.backend.util.Emails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

// RGPD-01 : la date de naissance n'est plus collectee (decision D-08). Les
// anciennes versions de l'app peuvent encore envoyer "dateNaissance" : le
// champ est ignore au lieu de faire echouer la requete.
@JsonIgnoreProperties(ignoreUnknown = true)
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

    // Email normalise des la lecture du JSON (espaces retires, minuscules) :
    // toutes les recherches en base se font ensuite sur cette forme unique.
    public void setEmail(String email) {
        this.email = Emails.normaliser(email);
    }

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    @TailleMaxBcrypt
    private String password;
}