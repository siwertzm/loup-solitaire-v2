package com.loupsolitaire.backend.config;

import java.security.Principal;
import java.util.UUID;

/**
 * Utilisateur authentifie de la requete en cours, tel que decrit par son
 * access token : uniquement son identifiant (UUID).
 *
 * Construit par JwtFilter directement a partir du token, SANS requete en
 * base : avant, chaque requete authentifiee rechargeait l'utilisateur. Les
 * controleurs le recoivent via @AuthenticationPrincipal et chargent
 * eux-memes ce dont ils ont besoin (souvent rien de plus : la verification
 * "ce personnage vous appartient" compare simplement des identifiants).
 *
 * Contrepartie : un compte supprime garde un access token valide jusqu'a son
 * expiration (15 minutes). Il ne peut rien en faire : ses personnages et son
 * compte n'existent plus (404), et il ne peut pas obtenir de nouveau token
 * (ses refresh tokens sont supprimes avec le compte).
 */
public record UtilisateurConnecte(UUID id) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}