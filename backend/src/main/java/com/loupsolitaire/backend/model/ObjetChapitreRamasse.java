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

// Combien d'exemplaires d'un objet OPTIONNEL un personnage a deja ramasse
// sur un chapitre donne (POST /personnages/{id}/objets/{objetId}). Sans ce
// suivi, rien n'empeche de reprendre le meme objet a l'infini sur le meme
// chapitre (le seul controle existant etait cote client, reinitialise a
// chaque rechargement de page).
//
// Une seule ligne par (personnage, chapitre, objet) : chaque ramassage
// incremente cette ligne plutot que d'en creer une nouvelle.
@Entity
@Table(
    name = "objet_chapitre_ramasse",
    uniqueConstraints = @UniqueConstraint(columnNames = {"personnage_id", "chapitre_id", "objet_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ObjetChapitreRamasse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnage_id", nullable = false)
    private Personnage personnage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_id", nullable = false)
    private Chapitre chapitre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objet_id", nullable = false)
    private Objet objet;

    @Column(nullable = false)
    private int quantite;
}