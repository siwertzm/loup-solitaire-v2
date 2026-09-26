package com.loupsolitaire.backend.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Discipline> resistances = new LinkedHashSet<>();

    // Disciplines que l'ennemi possede lui-meme (ex. Puissance Psychique des
    // Vordaks). Set et non List, comme resistances : les deux sont charges
    // dans le meme @EntityGraph que Combat.ennemis (voir CombatRepository),
    // et Hibernate refuse de fetcher deux "bags" (List sans @OrderColumn) en
    // une requete (MultipleBagFetchException). Combat.ennemis (List triee
    // par @OrderBy, REGLE-08) doit rester le seul bag de ce graphe.
    @ManyToMany
    @JoinTable(
        name = "ennemi_discipline",
        joinColumns = @JoinColumn(name = "ennemi_id"),
        inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Discipline> disciplines = new HashSet<>();
}