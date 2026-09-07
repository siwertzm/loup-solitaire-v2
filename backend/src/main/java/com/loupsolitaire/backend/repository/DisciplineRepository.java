package com.loupsolitaire.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.enums.IdDiscipline;

public interface DisciplineRepository extends JpaRepository<Discipline, IdDiscipline> {
}   