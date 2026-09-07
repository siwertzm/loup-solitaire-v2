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

// Objet trouve, perdu ou depense a un chapitre donne. Toujours possede par
// un seul Chapitre (jamais partage) -> ManyToOne + cascade cote Chapitre.
// L'Objet reference, lui, vient du catalogue partage (ManyToOne classique).
//
// valeur : peut etre negative (ex. paiement de 10 Couronnes -> valeur=-10
// sur l'objet "or"), voir chapitres 246/262 de chapitre.json.
// optionnel : true si le joueur peut choisir de ne pas prendre l'objet
// (la plupart des trouvailles), false si l'effet est automatique/obligatoire
// (ex. un paiement, un vol).
@Entity
@Table(name = "objet_chap")
@Getter
@Setter
@NoArgsConstructor
public class ObjetChap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_id", nullable = false)
    private Chapitre chapitre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objet_id", nullable = false)
    private Objet objet;

    @Column(nullable = false)
    private Integer valeur;

    @Column(nullable = false)
    private boolean optionnel;
}