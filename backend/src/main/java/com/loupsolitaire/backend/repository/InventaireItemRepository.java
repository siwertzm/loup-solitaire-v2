package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loupsolitaire.backend.model.InventaireItem;
import com.loupsolitaire.backend.model.Personnage;

public interface InventaireItemRepository extends JpaRepository<InventaireItem, UUID> {

    // JOIN FETCH : charge l'Objet en meme temps que la ligne d'inventaire,
    // pour eviter un LazyInitializationException quand le controleur lit
    // item.getObjet().getNom() une fois la session Hibernate fermee.
    @Query("SELECT i FROM InventaireItem i JOIN FETCH i.objet WHERE i.personnage = :personnage")
    List<InventaireItem> findByPersonnage(@Param("personnage") Personnage personnage);

    Optional<InventaireItem> findByPersonnageAndObjetId(Personnage personnage, String objetId);
}