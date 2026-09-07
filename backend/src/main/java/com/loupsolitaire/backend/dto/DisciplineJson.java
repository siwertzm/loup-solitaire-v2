package com.loupsolitaire.backend.dto;

import lombok.Getter;
import lombok.Setter;

// Un DTO par fichier source plutot que de desserialiser directement dans les
// entites JPA : evite les soucis de proxies/cascade Hibernate pendant le
// parsing, et rend le mapping JSON -> entite explicite et testable.
@Getter
@Setter
public class DisciplineJson {
    private String id;
    private String nom;
    private String description;
}