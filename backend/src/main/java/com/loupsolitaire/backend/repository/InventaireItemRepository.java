package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loupsolitaire.backend.model.InventaireItem;

public interface InventaireItemRepository extends JpaRepository<InventaireItem, UUID> {

    // JOIN FETCH : charge l'Objet en meme temps que la ligne d'inventaire,
    // pour eviter un LazyInitializationException quand le controleur lit
    // item.getObjet().getNom() une fois la session Hibernate fermee.
    //
    // Filtre par personnage.id (UUID scalaire) plutot que par reference
    // d'entite Personnage : cette derniere forme (WHERE i.personnage =
    // :personnage) declenchait un NullPointerException Hibernate
    // (EntityInitializerImpl.resolveInstanceSubInitializers, entityEntry
    // null) quand le Personnage passe en parametre avait deja ete charge
    // plus tot dans la meme transaction avec un JOIN FETCH sur une
    // collection (disciplines). Filtrer par id scalaire evite completement
    // cette resolution d'identite cote Hibernate.
    @Query("SELECT i FROM InventaireItem i JOIN FETCH i.objet WHERE i.personnage.id = :personnageId")
    List<InventaireItem> findByPersonnageId(@Param("personnageId") UUID personnageId);

    Optional<InventaireItem> findByPersonnageIdAndObjetId(UUID personnageId, String objetId);

    void deleteByPersonnageId(UUID personnageId);
}