package com.loupsolitaire.backend.util;

import java.util.Locale;

/**
 * Normalisation des adresses email.
 *
 * Une adresse email est stockee et recherchee TOUJOURS sous la meme forme :
 * sans espaces autour et en minuscules. Sans ca, "Bob@Mail.fr" et
 * "bob@mail.fr" pouvaient creer deux comptes distincts (la contrainte
 * UNIQUE de la base compare les chaines telles quelles), et un joueur qui
 * saisissait son email avec une autre casse qu'a l'inscription ne pouvait
 * ni se connecter ni reinitialiser son mot de passe.
 *
 * Locale.ROOT : evite les surprises de certaines langues (ex. le "I" turc).
 */
public final class Emails {

    private Emails() {
    }

    public static String normaliser(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}