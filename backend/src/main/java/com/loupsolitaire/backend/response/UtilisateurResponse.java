package com.loupsolitaire.backend.response;

import java.util.UUID;

import com.loupsolitaire.backend.model.Utilisateur;

// DTO expose au frontend : ne contient jamais le hash du mot de passe,
// contrairement au controleur /auth/me de la V1 qui renvoyait l'entite Utilisateur brute.
public record UtilisateurResponse(UUID id, String username) {

    public static UtilisateurResponse fromEntity(Utilisateur utilisateur) {
        return new UtilisateurResponse(utilisateur.getId(), utilisateur.getUsername());
    }
}
