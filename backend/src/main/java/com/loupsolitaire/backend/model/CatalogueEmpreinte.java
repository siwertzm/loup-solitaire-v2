package com.loupsolitaire.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DATA-00 : empreinte SHA-256 des JSON du livre deja charges en base. Une
// seule ligne (id = 1). GameDataLoader recharge le catalogue quand
// l'empreinte des fichiers ne correspond plus (voir 015-catalogue-empreinte.sql).
@Entity
@Table(name = "catalogue_empreinte")
@Getter
@Setter
@NoArgsConstructor
public class CatalogueEmpreinte {

    public static final int ID_UNIQUE = 1;

    @Id
    private Integer id = ID_UNIQUE;

    @Column(nullable = false, length = 64)
    private String empreinte;

    @Column(name = "charge_le", nullable = false)
    private Instant chargeLe;
}
