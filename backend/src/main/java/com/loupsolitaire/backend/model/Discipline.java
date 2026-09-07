package com.loupsolitaire.backend.model;

import com.loupsolitaire.backend.model.enums.IdDiscipline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Identifiant metier (enum), pas d'UUID : reference directement dans le
// contenu du livre (discipline.json, resistance des ennemis, etc.).
@Entity
@Table(name = "discipline")
@Getter
@Setter
@NoArgsConstructor
public class Discipline {

    @Id
    @Enumerated(EnumType.STRING)
    private IdDiscipline id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 3000, nullable = false)
    private String description;
}