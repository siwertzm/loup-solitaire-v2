package com.loupsolitaire.backend.model;

import com.loupsolitaire.backend.model.enums.IdDiscipline;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
//
// Donnee du catalogue (livre), identique pour tous les joueurs et jamais
// modifiee en jeu : gardee en cache de second niveau Hibernate (voir
// application.properties) pour ne pas la relire en base a chaque requete.
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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