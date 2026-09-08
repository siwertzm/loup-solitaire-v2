package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;

public interface InventaireItemRepository extends JpaRepository<InventaireItem, UUID> {

    // Pour la fiche personnage : tout son inventaire.
    List<InventaireItem> findByPersonnage(Personnage personnage);

    // Pour savoir si un objet est deja possede, avant d'incrementer sa
    // quantite plutot que de creer une nouvelle ligne.
    Optional<InventaireItem> findByPersonnageAndObjetId(Personnage personnage, String objetId);
}