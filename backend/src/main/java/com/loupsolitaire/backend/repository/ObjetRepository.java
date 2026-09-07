package com.loupsolitaire.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Objet;

public interface ObjetRepository extends JpaRepository<Objet, String> {
}