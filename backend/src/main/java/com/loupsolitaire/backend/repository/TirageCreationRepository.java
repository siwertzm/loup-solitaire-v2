package com.loupsolitaire.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loupsolitaire.backend.model.TirageCreation;

public interface TirageCreationRepository extends JpaRepository<TirageCreation, UUID> {
}