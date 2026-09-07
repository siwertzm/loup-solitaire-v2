package com.loupsolitaire.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.loupsolitaire.backend.model.enums.TypeEffet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Jamais partage entre deux Objet/Chapitre : toujours possede par un seul
// parent. D'ou les deux colonnes de cle etrangere nullables ci-dessous
// (une seule remplie a la fois) plutot qu'une relation ManyToMany comme en V1.
@Entity
@Table(name = "effet")
@Getter
@Setter
@NoArgsConstructor
public class Effet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TypeEffet type;

    private Integer valeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objet_id")
    private Objet objet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_id")
    private Chapitre chapitre;

    @OneToMany(mappedBy = "effet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cond> conditions = new ArrayList<>();
}