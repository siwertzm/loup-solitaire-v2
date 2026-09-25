package com.loupsolitaire.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loupsolitaire.backend.model.Utilisateur;

import jakarta.persistence.LockModeType;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByUsername(String username);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Verrou d'ecriture (SELECT ... FOR UPDATE) sur la ligne de l'utilisateur,
    // jusqu'a la fin de la transaction. Sert au tirage de creation : deux
    // requetes simultanees (double tap sur les deux des) passent l'une apres
    // l'autre, si bien que la seconde relit le tirage de la premiere au lieu
    // d'en inserer un deuxieme (violation de cle primaire, erreur 500).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Utilisateur u WHERE u.id = :id")
    Optional<Utilisateur> findByIdPourModification(@Param("id") UUID id);
}