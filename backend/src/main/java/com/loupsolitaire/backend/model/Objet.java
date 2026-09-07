package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.loupsolitaire.backend.model.enums.CategorieObjet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Identifiant metier (String, slug) : reference directement dans chapitre.json
// (objets ramassables) et dans l'inventaire du joueur (a venir).
@Entity
@Table(name = "objet")
@Getter
@Setter
@NoArgsConstructor
public class Objet {

    @Id
    private String id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieObjet categorie;

    // Effets propres a l'objet (ex. le casque donne +2 endurance). Jamais
    // partages avec un autre objet : possession complete (cascade + orphelins).
    @OneToMany(mappedBy = "objet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Effet> effets = new ArrayList<>();
}