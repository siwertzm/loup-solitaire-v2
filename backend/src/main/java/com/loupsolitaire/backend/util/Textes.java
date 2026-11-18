package com.loupsolitaire.backend.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * DATA-19 : langue des textes du livre, choisie par l'en-tete X-Langue que
 * l'app envoie avec chaque appel (langue.interceptor.ts).
 *
 * Pas Accept-Language : les WebView l'envoient d'office avec la langue du
 * telephone, et les apps deja installees (interface forcee en francais)
 * recevraient le livre en anglais. Sans en-tete, ou hors requete HTTP
 * (tache planifiee, email), le francais.
 */
public final class Textes {

    public static final String ENTETE_LANGUE = "X-Langue";

    private Textes() {
    }

    /**
     * Texte anglais si la requete le demande et qu'il existe (colonne remplie,
     * meme vide : une description absente de la VO reste vide), sinon francais.
     */
    public static String selon(String francais, String anglais) {
        return anglais != null && "en".equals(langueDemandee()) ? anglais : francais;
    }

    private static String langueDemandee() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            return attributs.getRequest().getHeader(ENTETE_LANGUE);
        }
        return null;
    }
}
