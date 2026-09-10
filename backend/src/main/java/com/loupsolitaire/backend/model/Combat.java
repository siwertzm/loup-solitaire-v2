package com.loupsolitaire.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.loupsolitaire.backend.model.enums.StatutCombat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Etat persiste d'un combat : cree a l'arrivee sur un Chapitre.combat=true
// (voir CombatService.initierCombat), un par (personnage, chapitre). Server
// fait autorite : c'est CE record, jamais l'etat envoye par le client, qui
// determine l'issue et alimente les conditions FUITE/ASSAUT_MAX/
// ASSAUT_ECHEC/ENDURANCE_PERDUE (voir ConditionService).
//
// L'endurance du JOUEUR n'est pas dupliquee ici : elle reste directement sur
// Personnage.enduranceActuelle, toujours reelle des le premier coup encaisse
// (pas de "vie de combat" separee, coherent avec le reste du jeu).
@Entity
@Table(name = "combat")
@Getter
@Setter
@NoArgsConstructor
public class Combat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnage_id", nullable = false)
    private Personnage personnage;

    // Chapitre d'origine (celui qui a declenche ce combat). Sert a
    // retrouver le Combat courant/le plus recent d'un personnage, et a
    // savoir depuis quel Chapitre evaluer les conditions de Lien.
    @Column(nullable = false)
    private Integer chapitreId;

    @OneToMany(mappedBy = "combat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "ordre_liste")
    private List<CombatEnnemi> ennemis = new ArrayList<>();

    // Index dans "ennemis" de l'adversaire actuellement affronte. Avance
    // d'un cran des que ennemis.get(ennemiActifIndex).enduranceActuelle
    // tombe a 0 ou moins ; le combat passe en VICTOIRE quand il depasse le
    // dernier ennemi de la liste.
    @Column(nullable = false)
    private int ennemiActifIndex;

    // Total d'assauts livres sur l'ensemble du combat (tous ennemis
    // confondus) : ATTAQUE/DEFENSE/OBJET comptent chacun comme un assaut
    // (ils declenchent tous une riposte de l'ennemi) ; FUITE non. Alimente
    // les conditions ASSAUT_MAX/ASSAUT_ECHEC/FUITE du Lien de sortie.
    @Column(nullable = false)
    private int assautsLivres;

    // true des que le joueur a perdu au moins 1 point d'ENDURANCE au cours
    // de CE combat (tous ennemis confondus). Alimente la condition
    // ENDURANCE_PERDUE (ex. chapitre 227 : "si vous tuez sans perdre
    // aucun point d'ENDURANCE, rendez-vous au 348").
    @Column(nullable = false)
    private boolean endurancePerdue;

    // Bonus d'HABILETE tire lors d'une DEFENSE, qui reste disponible pour
    // la PROCHAINE action ATTAQUE (voir ActionCombat). Remis a 0 des qu'il
    // est consomme par une attaque, ou qu'un nouveau DEFENSE l'ecrase.
    @Column(nullable = false)
    private int bonusHabiliteEnAttente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCombat statut;

    // Sert a retrouver le combat le PLUS RECENT d'un personnage sur un
    // chapitre (CombatRepository) : l'id UUID (genere aleatoirement, pas
    // "time-based") ne peut pas etre utilise pour ordonner dans le temps.
    @Column(nullable = false)
    private Instant creeLe;

    // Ennemi actuellement affronte, ou null si tous sont vaincus (combat
    // termine). Pratique cote service pour eviter de repeter l'acces a la
    // liste + l'index partout.
    public CombatEnnemi getEnnemiActif() {
        if (ennemiActifIndex >= ennemis.size()) {
            return null;
        }
        return ennemis.get(ennemiActifIndex);
    }
}