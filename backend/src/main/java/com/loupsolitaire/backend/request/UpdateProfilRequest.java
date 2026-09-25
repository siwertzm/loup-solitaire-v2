package com.loupsolitaire.backend.request;

import java.time.LocalDate;
 
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
 
// Tous les champs sont optionnels : seuls ceux fournis (non null) sont mis a jour.
@Getter
@Setter
public class UpdateProfilRequest {

    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caracteres")
    // Interdit "@" : le login accepte le nom d'utilisateur OU l'email. Un nom
    // contenant "@" pourrait reprendre l'email d'un autre joueur et rendre
    // la connexion ambigue (voir CustomUserDetailsService).
    @Pattern(regexp = "^[^@]*$", message = "Le nom d'utilisateur ne peut pas contenir le caractere @")
    private String username;
    
    @Email(message = "Format d'email invalide")
    private String email;

 
    @Past(message = "La date de naissance doit etre dans le passe")
    private LocalDate dateNaissance;
}