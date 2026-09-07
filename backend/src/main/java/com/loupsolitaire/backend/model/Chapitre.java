package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Identifiant metier (Integer, pas generate) : correspond au numero de
// paragraphe du livre-jeu, reference directement par Lien.page.
@Entity
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
    @ManyToMany
    @JoinTable(
        name = "chapitre_ennemi",
        joinColumns = @JoinColumn(name = "chapitre_id"),
        inverseJoinColumns = @JoinColumn(name = "ennemi_id")
    )
    private List<Ennemi> ennemis = new ArrayList<>();

    // Effets, liens et objets ramassables : toujours propres a UN SEUL
    // chapitre (jamais partages) -> possession complete (cascade + orphelins).
    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Effet> effets = new ArrayList<>();

    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lien> liens = new ArrayList<>();

    @OneToMany(mappedBy = "chapitre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObjetChap> objets = new ArrayList<>();
}