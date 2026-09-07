package com.loupsolitaire.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CondJson {
    private String type;
    private String targetId;
    // Toujours une String cote source : contient soit un nombre ("10"),
    // soit une plage encodee ("[0, 4]"). Ne jamais typer en Integer.
    private String valeur;
}