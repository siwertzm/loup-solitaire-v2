package com.loupsolitaire.backend.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// RGPD-02 : export de toutes les donnees du compte (droit d'acces et de
// portabilite), envoye par email en piece jointe JSON. Construit par
// ExportDonneesService.
//
// Exclus volontairement : mot de passe hache et jetons (session, verification
// d'email, reinitialisation) = secrets de securite ; champs techniques
// (version de verrouillage, tirage de hasard du chapitre en cours) ; logs
// serveur et Sentry, hors base, traites a la main sur demande.
public record ExportDonneesResponse(
        Instant dateExport,
        Compte compte,
        TirageCreationEnCours tirageCreationEnCours,
        List<PersonnageExporte> personnages) {

    public record Compte(
            String nomUtilisateur,
            String email,
            boolean emailVerifie,
            String emailEnAttente,
            Instant dateCreation) {
    }

    public record TirageCreationEnCours(int hasardHabilete, int hasardEndurance, Instant date) {
    }

    public record PersonnageExporte(
            UUID id,
            String nom,
            Instant dateCreation,
            Instant derniereActivite,
            int habileteBase,
            int habilete,
            int enduranceMax,
            int enduranceActuelle,
            List<String> disciplines,
            String armeMaitrisee,
            Integer chapitreActuel,
            Integer chapitrePrecedent,
            boolean mort,
            List<Integer> chapitresParcourus,
            List<ObjetPossede> inventaire,
            List<ObjetRamasse> objetsRamasses,
            List<CombatExporte> combats) {
    }

    public record ObjetPossede(String objet, int quantite) {
    }

    public record ObjetRamasse(Integer chapitre, String objet, int quantite) {
    }

    public record CombatExporte(
            Integer chapitre,
            String statut,
            int assautsLivres,
            List<String> ennemis,
            Instant date) {
    }
}
