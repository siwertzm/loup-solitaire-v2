package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Identifiant metier (String, slug) : reference directement dans chapitre.json.
@Entity
@Table(name = "ennemi")
@Getter
@Setter
@NoArgsConstructor
public class Ennemi {

    @Id
    private String id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int habilite;

    @Column(nullable = false)
    private int endurance;

    // Une Discipline est un catalogue partage : plusieurs ennemis peuvent
    // resister a la meme discipline (ex. puissance_psychique). ManyToMany
    // est ici le bon choix, contrairement a Effet/Cond qui ne sont jamais
    // partages entre parents.
    @ManyToMany
    @JoinTable(
        name = "ennemi_resistance",
        joinColumns = @JoinColumn(name = "ennemi_id"),
        inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    private List<Discipline> resistances = new ArrayList<>();
}