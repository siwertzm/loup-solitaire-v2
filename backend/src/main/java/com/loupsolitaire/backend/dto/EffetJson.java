package com.loupsolitaire.backend.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EffetJson {
    private String type;
    private Integer valeur;
    private String nom;
    private List<CondJson> cond;
}