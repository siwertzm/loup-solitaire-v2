package com.loupsolitaire.backend.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapitreJson {
    private Integer id;
    private String text;
    private boolean combat;
    private List<RefJson> ennemi;
    private List<EffetJson> effet;
    private List<LienJson> lien;
    private List<ObjetChapJson> objet;
}