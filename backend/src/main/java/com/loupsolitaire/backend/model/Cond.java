package com.loupsolitaire.backend.model;

import java.util.UUID;

import com.loupsolitaire.backend.model.enums.TypeCondition;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Jamais partagee entre plusieurs Effet/Lien : toujours possedee par un seul
// parent (voir doc de conception). Le rattachement a Lien sera ajoute quand
// on traitera chapitre.json (les liens de chapitre ont aussi des conditions).
@Entity
@Table(name = "cond")
@Getter
@Setter
@NoArgsConstructor
public class Cond {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TypeCondition type;

    private String targetId;

    private String valeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effet_id")
    private Effet effet;
}