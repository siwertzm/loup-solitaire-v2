package com.loupsolitaire.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;

public interface PersonnageRepository extends JpaRepository<Personnage, UUID> {

    // Pour l'ecran profil : la liste des personnages d'un utilisateur.
    List<Personnage> findByUtilisateur(Utilisateur utilisateur);
}