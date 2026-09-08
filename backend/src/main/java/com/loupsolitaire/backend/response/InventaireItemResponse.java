package com.loupsolitaire.backend.response;

public record InventaireItemResponse(
        String objetId,
        String nom,
        String categorie,
        int quantite) {
}