package com.loupsolitaire.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "password_reset_token",
        indexes = @Index(
                name = "idx_password_reset_utilisateur",
                columnList = "utilisateur_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    // Hash BCrypt du code à 6 chiffres.
    // Le code brut n'est jamais enregistré en base.
    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean utilise = false;

    @Column(nullable = false)
    private int tentatives = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}