package com.loupsolitaire.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generation et hachage des tokens envoyes aux utilisateurs (refresh token,
 * lien de verification d'email, token de reinitialisation du mot de passe).
 *
 * Le token brut n'est donne qu'une fois au client (ou par email) : la base ne
 * conserve que son SHA-256. Un token de 256 bits aleatoires n'a pas besoin
 * d'un hachage lent (BCrypt) : il est impossible a deviner, et SHA-256
 * permet de le retrouver en base par simple egalite.
 */
public final class Tokens {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OCTETS_ALEATOIRES = 32;

    private Tokens() {
    }

    // 32 octets aleatoires en Base64 "URL safe" sans padding : utilisable
    // tel quel dans un lien (?token=...) ou un corps JSON.
    public static String genererAleatoire() {
        byte[] bytes = new byte[OCTETS_ALEATOIRES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Code numerique a 6 chiffres (000000 a 999999), saisi a la main par
    // l'utilisateur : peu d'entropie, il est donc hache avec BCrypt et le
    // nombre d'essais est limite (voir PasswordResetService).
    public static String genererCodeASixChiffres() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    // Empreinte SHA-256 en hexadecimal (64 caracteres), stockee en base.
    public static String hacher(String valeur) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Tout JDK doit fournir SHA-256 : ne peut pas arriver en pratique.
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}