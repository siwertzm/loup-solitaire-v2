package com.loupsolitaire.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Chapitre;

public interface ChapitreRepository extends JpaRepository<Chapitre, Integer> {
}