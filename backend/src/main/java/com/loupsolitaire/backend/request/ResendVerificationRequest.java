package com.loupsolitaire.backend.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import com.loupsolitaire.backend.util.Emails;

import lombok.Getter;
import lombok.Setter;

/**
 * Renvoi du lien de verification.
 *
 * Deux formes acceptees :
 * - {"email": "..."} : ecran Profil (et anciennes versions de l'app) ;
 * - {"identifiant": "..."} : ecran de connexion, avec ce que le joueur a
 *   tape (pseudo OU email). Le backend retrouve l'email du compte en base,
 *   le joueur n'a pas a le ressaisir.
 * Si les deux sont fournis, l'email est prioritaire.
 */
@Getter
@Setter
public class ResendVerificationRequest {

    @Email(message = "Format d'email invalide")
    private String email;

    private String identifiant;

    // Email normalise des la lecture du JSON (espaces retires, minuscules) :
    // toutes les recherches en base se font ensuite sur cette forme unique.
    public void setEmail(String email) {
        this.email = Emails.normaliser(email);
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant == null ? null : identifiant.trim();
    }

    @AssertTrue(message = "L'email ou l'identifiant est obligatoire")
    public boolean isRenseigne() {
        return (email != null && !email.isBlank())
                || (identifiant != null && !identifiant.isBlank());
    }

    /** Ce qui sert a retrouver le compte : l'email s'il est fourni, sinon l'identifiant. */
    public String identifiantOuEmail() {
        return email != null && !email.isBlank() ? email : identifiant;
    }
}