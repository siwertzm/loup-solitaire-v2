package com.loupsolitaire.backend.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Les tirages d'HABILETE et d'ENDURANCE ne font plus partie de la requete :
// ils sont faits par le serveur (POST /personnages/tirage, voir
// TirageCreationService). Un ancien client qui les enverrait encore n'a
// aucun effet : ces champs inconnus sont ignores explicitement (et non
// rejetes en 400), pour que les versions de l'app deja installees
// continuent de fonctionner.
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class CreerPersonnageRequest {

    @NotBlank(message = "Le nom du personnage est obligatoire")
    @Size(max = 40, message = "Le nom du personnage ne peut pas depasser 40 caracteres")
    private String nom;

    // Noms exacts des valeurs de l'enum IdDiscipline (ex. "CAMOUFLAGE",
    // "MAITRISE_ARMES"). Exactement 5, revalide cote service.
    @NotNull(message = "Il faut choisir exactement 5 disciplines")
    @Size(min = 5, max = 5, message = "Il faut choisir exactement 5 disciplines")
    private List<String> disciplines;
}