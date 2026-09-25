package com.loupsolitaire.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Tirage des caracteristiques en attente de la creation d'un personnage
// (regles du livre : HABILETE = 10 + tirage, ENDURANCE = 20 + tirage).
//
// Tire par le SERVEUR (POST /personnages/tirage), jamais par le client : le
// joueur ne peut ni choisir ses chiffres ni relancer. Une seule ligne par
// utilisateur (cle = son identifiant) : redemander un tirage renvoie le
// meme, meme apres avoir quitte l'ecran. La ligne est supprimee quand le
// personnage est cree (PartieService.creerPersonnage), ce qui permet un
// nouveau tirage pour le personnage suivant.
//
// Supprimee aussi avec le compte : cle etrangere ON DELETE CASCADE (voir
// 011-tirage-creation.sql).
@Entity
@Table(name = "tirage_creation")
@Getter
@Setter
@NoArgsConstructor
public class TirageCreation {

    @Id
    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    // Chiffres bruts 0-9 de la Table de Hasard.
    @Column(nullable = false)
    private int hasardHabilite;

    @Column(nullable = false)
    private int hasardEndurance;

    @Column(nullable = false)
    private Instant creeLe;
}