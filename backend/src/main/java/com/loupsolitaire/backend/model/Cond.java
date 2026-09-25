package com.loupsolitaire.backend.model;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.loupsolitaire.backend.model.enums.TypeCondition;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Jamais partagee entre plusieurs Effet/Lien : toujours possedee par un seul
// parent (voir doc de conception). Le rattachement a Lien sera ajoute quand
// on traitera chapitre.json (les liens de chapitre ont aussi des conditions).
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "cond")
@Getter
@Setter
@NoArgsConstructor
public class Cond {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TypeCondition type;

    private String targetId;

    private String valeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effet_id")
    private Effet effet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lien_id")
    private Lien lien;

    // Plage de HASARD : "[min, max]", chaque borne etant un chiffre de 0 a 9
    // (resultat de la Table de Hasard).
    private static final Pattern FORMAT_PLAGE = Pattern.compile("\\[\\s*(\\d)\\s*,\\s*(\\d)\\s*\\]");

    // Lecture STRICTE des valeurs : une valeur mal ecrite leve une erreur au
    // lieu de valoir 0 en silence (ce qui rendait un lien ou un effet
    // toujours disponible, ou jamais, sans que personne ne s'en apercoive).
    // Les donnees du livre sont verifiees au chargement (GameDataLoader) avec
    // ces memes methodes : en jeu, elles ne peuvent donc plus echouer.

    // Valeur numerique (quantite, seuil d'endurance, nombre d'assauts...).
    public int valeurEntiere() {
        if (valeur == null) {
            throw new IllegalStateException("Valeur manquante pour la condition " + type);
        }
        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Valeur non entiere pour la condition " + type + " : \"" + valeur + "\"", e);
        }
    }

    // Plage d'une condition HASARD (ex. "[0, 4]").
    public PlageHasard plageHasard() {
        Matcher matcher = FORMAT_PLAGE.matcher(valeur == null ? "" : valeur.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "Plage de hasard invalide (attendu \"[min, max]\" entre 0 et 9) : \"" + valeur + "\"");
        }
        int min = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));
        if (min > max) {
            throw new IllegalStateException("Plage de hasard inversee : \"" + valeur + "\"");
        }
        return new PlageHasard(min, max);
    }

    public record PlageHasard(int min, int max) {

        public boolean contient(int tirage) {
            return tirage >= min && tirage <= max;
        }
    }
}