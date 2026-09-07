package com.loupsolitaire.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Version MINIMALE pour l'instant : juste assez pour qu'Effet puisse s'y
// rattacher (voir GameDataLoader, chargement des objets). Sera completee
// (text, combat, liens, objets, ennemis) au moment de traiter chapitre.json.
@Entity
@Table(name = "chapitre")
@Getter
@Setter
@NoArgsConstructor
public class Chapitre {

    @Id
    private Integer id;
}