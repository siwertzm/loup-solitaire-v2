package com.loupsolitaire.backend.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreerPersonnageRequest {

    @NotBlank(message = "Le nom du personnage est obligatoire")
    private String nom;

    @NotNull(message = "Le hasard est obligatoire")
    private Integer hasardHabilite;

    @NotNull(message = "Le hasard est obligatoire")
    private Integer hasardEndurance;

    // Noms exacts des valeurs de l'enum IdDiscipline (ex. "CAMOUFLAGE",
    // "MAITRISE_ARMES"). Exactement 5, revalide cote service.
    @Size(min = 5, max = 5, message = "Il faut choisir exactement 5 disciplines")
    private List<String> disciplines;
}