package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un chemin sortant d'un Chapitre vers un autre. Toujours possede par un
// seul Chapitre (jamais partage) : contrairement a la V1 qui utilisait un
// ManyToMany Chapitre<->Lien, ici c'est un vrai ManyToOne + cascade.
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "lien")
@Getter
@Setter
@NoArgsConstructor
public class Lien {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Le chapitre source (celui qui possede ce lien).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_id", nullable = false)
    private Chapitre chapitre;

    // Le chapitre cible. Toujours resolu et non-null en base : les liens
    // vers un numero de chapitre inexistant (fins de partie/de tome) sont
    // ignores des le chargement, voir GameDataLoader.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_cible_id", nullable = false)
    private Chapitre chapitreCible;

    @OneToMany(mappedBy = "lien", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Cond> conditions = new ArrayList<>();
}