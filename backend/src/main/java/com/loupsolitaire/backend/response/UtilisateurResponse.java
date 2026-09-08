package com.loupsolitaire.backend.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.loupsolitaire.backend.model.Utilisateur;

// DTO expose au frontend : ne contient jamais le hash du mot de passe,
// contrairement au controleur /auth/me de la V1 qui renvoyait l'entite Utilisateur brute.
// "personnages" : la liste des personnages jouables de l'utilisateur, pour
// l'ecran profil. Vide (jamais null) si aucun personnage cree.
public record UtilisateurResponse(
        UUID id,
        String username,
        String email,
        LocalDate dateNaissance,
        boolean emailVerifie,
        List<PersonnageResponse> personnages) {

    // Pour les endpoints qui n'ont pas besoin de la liste des personnages
    // (register, updateProfil) : evite une requete inutile.
    public static UtilisateurResponse fromEntity(Utilisateur utilisateur) {
        return fromEntity(utilisateur, List.of());
    }

    public static UtilisateurResponse fromEntity(Utilisateur utilisateur, List<PersonnageResponse> personnages) {
        return new UtilisateurResponse(
                utilisateur.getId(),
                utilisateur.getUsername(),
                utilisateur.getEmail(),
                utilisateur.getDateNaissance(),
                utilisateur.isEmailVerifie(),
                personnages
        );
    }
}