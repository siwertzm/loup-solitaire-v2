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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un objet du catalogue possede par un Personnage, avec sa quantite. L'or
// est ici un objet comme un autre (categorie BOURSE), pas un champ dedie sur
// Personnage : coherent avec objet.json, ou "or" et "pierre_precieuse" sont
// des Objet normaux.
//
// Une seule ligne par (personnage, objet) : "prendre" un objet deja possede
// incremente sa quantite plutot que de creer une deuxieme ligne (voir le
// service d'inventaire, a venir).
@Entity
@Table(
    name = "inventaire_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"personnage_id", "objet_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class InventaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnage_id", nullable = false)
    private Personnage personnage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objet_id", nullable = false)
    private Objet objet;

    @Column(nullable = false)
    private int quantite;
}