package com.loupsolitaire.backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "utilisateur",
    uniqueConstraints = @UniqueConstraint(name = "uk_utilisateur_username", columnNames = "username")
)
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    // Hash BCrypt uniquement. Ne jamais serialiser ce champ vers le frontend
    // (voir UtilisateurPublicDTO cote reponse, a creer avec le controleur /me).
    @Column(nullable = false)
    private String password;
}
