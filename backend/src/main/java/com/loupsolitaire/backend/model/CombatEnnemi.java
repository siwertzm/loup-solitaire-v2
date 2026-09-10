package com.loupsolitaire.backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Instance d'un Ennemi (catalogue partage, stats fixes) au sein d'UN combat
// precis. Necessaire car l'endurance evolue pendant le combat et parce
// qu'un meme Ennemi du catalogue peut apparaitre plusieurs fois dans le
// meme Chapitre (ex. chapitre 253 : 4x "loup_maudit") : chaque occurrence
// doit pouvoir encaisser des degats independamment des autres.
@Entity
@Table(name = "combat_ennemi")
@Getter
@Setter
@NoArgsConstructor
public class CombatEnnemi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combat_id", nullable = false)
    private Combat combat;

    // Reference au catalogue : source des stats de base (habilite,
    // endurance max, resistances). Jamais modifie pendant le combat.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ennemi_id", nullable = false)
    private Ennemi ennemi;

    // Ordre d'affrontement au sein du combat (0 = premier). Les ennemis
    // d'un meme chapitre se combattent sequentiellement, jamais en meme
    // temps (voir chapitre.json : "vous devrez les affronter a tour de
    // role").
    @Column(nullable = false)
    private int ordre;

    @Column(nullable = false)
    private int enduranceActuelle;
}