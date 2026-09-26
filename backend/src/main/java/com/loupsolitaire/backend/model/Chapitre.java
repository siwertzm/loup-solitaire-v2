package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Identifiant metier (Integer, pas generate) : correspond au numero de
// paragraphe du livre-jeu, reference directement par Lien.page.
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "chapitre")
@Getter
@Setter
@NoArgsConstructor
public class Chapitre {

    @Id
    private Integer id;

    // TEXT plutot que VARCHAR : certains paragraphes depassent largement 255
    // caracteres (plus de 2000 pour les plus longs).
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean combat;

    // Catalogue partage : plusieurs chapitres peuvent faire combattre le
    // meme ennemi (ex. plusieurs "Glok" identiques a differents endroits).
    // REGLE-08 : l'ordre de la liste est l'ordre d'affrontement (colonne
    // chapitre_ennemi.ordre, maintenue par Hibernate).
    @ManyToMany
    @JoinTable(
        name = "chapitre_ennemi",
        joinColumns = @JoinColumn(name = "chapitre_id"),
        inverseJoinColumns = @JoinColumn(name = "ennemi_id")
    )
    @OrderColumn(name = "ordre")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Ennemi> ennemis = new ArrayList<>();

    // Effets, liens et objets ramassables : toujours propres a UN SEUL
    // chapitre (jamais partages) -> possession complete (cascade + orphelins).
    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Effet> effets = new ArrayList<>();

    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Lien> liens = new ArrayList<>();

    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<ObjetChap> objets = new ArrayList<>();
}