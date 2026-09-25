package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.loupsolitaire.backend.model.enums.CategorieObjet;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Effet> effets = new ArrayList<>();
}