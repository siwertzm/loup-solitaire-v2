package com.loupsolitaire.backend.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LienJson {
    // String cote source : peut valoir "0", "351", "352" (fins de partie/de
    // tome, ignorees au chargement) en plus des vrais numeros de chapitre.
    private String page;
    private List<CondJson> cond;
}