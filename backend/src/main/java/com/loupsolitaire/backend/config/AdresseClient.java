package com.loupsolitaire.backend.config;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Adresse IP reelle du client, pour la limitation de debit (SEC-02).
 *
 * Derriere le proxy de Render, getRemoteAddr() renvoie l'adresse du proxy :
 * toutes les requetes auraient la meme IP et partageraient les memes
 * compteurs. On lit donc un en-tete pose par le proxy, en se mefiant de
 * X-Forwarded-For : sa partie GAUCHE est envoyee par le client et peut etre
 * inventee (il suffirait de la changer a chaque requete pour contourner la
 * limite). Chaque proxy de confiance AJOUTE une adresse a droite : avec N
 * proxys de confiance, l'adresse fiable du client est la N-ieme en partant
 * de la droite.
 */
@Component
@RequiredArgsConstructor
public class AdresseClient {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final LimitationDebitProperties proprietes;

    public String de(HttpServletRequest requete) {
        String entete = proprietes.enteteIpClient();
        if (entete != null && !entete.isBlank()) {
            String valeur = requete.getHeader(entete);
            if (valeur != null && !valeur.isBlank()) {
                return valeur.trim();
            }
        }

        int proxies = proprietes.proxiesDeConfiance();
        String xff = requete.getHeader(X_FORWARDED_FOR);
        if (proxies > 0 && xff != null && !xff.isBlank()) {
            String[] adresses = xff.split(",");
            int index = Math.max(0, adresses.length - proxies);
            return adresses[index].trim();
        }
        return requete.getRemoteAddr();
    }
}