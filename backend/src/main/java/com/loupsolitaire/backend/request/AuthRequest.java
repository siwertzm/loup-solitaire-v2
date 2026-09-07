package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {

    // Accepte soit le nom d'utilisateur, soit l'email (voir CustomUserDetailsService).
    @NotBlank(message = "Le nom d'utilisateur ou l'email est obligatoire")
    private String identifiant;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
