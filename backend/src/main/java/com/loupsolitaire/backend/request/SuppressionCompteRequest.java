package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Meme principe que ChangementMotDePasseRequest : on exige le mot de passe actuel
// avant une action destructrice et irreversible (voir AuthController.supprimerCompte).
@Getter
@Setter
public class SuppressionCompteRequest {

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}