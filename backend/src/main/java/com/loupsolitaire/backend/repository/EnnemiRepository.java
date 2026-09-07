package com.loupsolitaire.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Ennemi;

public interface EnnemiRepository extends JpaRepository<Ennemi, String> {
}