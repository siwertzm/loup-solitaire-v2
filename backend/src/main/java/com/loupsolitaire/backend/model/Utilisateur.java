package com.loupsolitaire.backend.model;

import java.time.LocalDate;
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
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_utilisateur_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email")
    }
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

    @Column(nullable = false, unique = true)
    private String email;

    // Optionnelle : peut etre completee apres inscription via PUT /auth/me.
    // Date de naissance plutot qu'un age en dur, qui deviendrait faux avec le temps.
    private LocalDate dateNaissance;

    // Passe a true uniquement apres clic sur le lien de confirmation recu par email.
    // Le login est bloque tant que ce flag est false (voir AuthController.login).
    @Column(nullable = false)
    private boolean emailVerifie = false;
}