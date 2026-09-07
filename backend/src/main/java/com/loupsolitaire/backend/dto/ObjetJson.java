package com.loupsolitaire.backend.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjetJson {
    private String id;
    private String nom;
    private String description;
    private String categorie;
    private List<EffetJson> effet;
}