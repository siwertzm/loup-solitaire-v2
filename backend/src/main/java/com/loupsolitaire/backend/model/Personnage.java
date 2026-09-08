package com.loupsolitaire.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Le personnage jouable cree par un utilisateur (Feuille d'Aventure).
// L'inventaire (or compris - categorie BOURSE d'Objet, comme n'importe quel
// autre objet) n'est PAS ici : c'est une entite a part, a venir juste apres.
@Entity
@Table(name = "personnage")
@Getter
@Setter
@NoArgsConstructor
public class Personnage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String nom;

    // Valeur fixee a la creation (10 + tirage), jamais modifiee ensuite
    // (sauf de rares effets permanents comme le chapitre 236 de ce tome).
    // "habilite" ci-dessous est la valeur COURANTE/EFFECTIVE, recalculee a
    // chaque ajout/retrait d'arme (voir InventaireService).
    @Column(nullable = false)
    private int habiliteBase;

    // Valeur effective actuelle : habiliteBase ajustee selon les armes
    // possedees (-4 sans arme, +2 avec l'arme maitrisee, +0 sinon). C'est
    // celle-ci qu'on utilise en jeu, jamais habiliteBase directement.
    @Column(nullable = false)
    private int habilite;

    // Bonus/malus TEMPORAIRE d'habilite (ex. essence d'Alether), remis a
    // zero a chaque changement de chapitre. Pas d'equivalent pour
    // l'endurance : toute perte/gain d'ENDURANCE est toujours reel, jamais
    // temporaire (sauf le cas des objets speciaux, qui touchent aussi le
    // plafond enduranceMax - voir ObjetService).
    @Column(nullable = false)
    private int habiliteTemp;

    // Plafond fixe a la creation (20 + tirage), rarement modifie en cours
    // de partie. A distinguer de enduranceActuelle qui fluctue sans cesse.
    @Column(nullable = false)
    private int enduranceMax;

    @Column(nullable = false)
    private int enduranceActuelle;

    // Exactement 5 parmi les 10 disponibles (regle validee au niveau du
    // service de creation de personnage, pas au niveau de l'entite).
    @ManyToMany
    @JoinTable(
        name = "personnage_discipline",
        joinColumns = @JoinColumn(name = "personnage_id"),
        inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    private List<Discipline> disciplines = new ArrayList<>();

    // Rempli uniquement si la Discipline Kai "Maitrise des Armes" a ete
    // choisie (tirage uniforme parmi les 9 armes du catalogue).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arme_maitrisee_id")
    private Objet armeMaitrisee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_actuel_id", nullable = false)
    private Chapitre chapitreActuel;

    // D'ou vient le personnage : utile pour l'affichage ("retour"), propre
    // a chaque personnage plutot qu'une propriete generique du Chapitre
    // (plusieurs chemins peuvent mener au meme chapitre).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapitre_precedent_id")
    private Chapitre chapitrePrecedent;

    @Column(nullable = false)
    private Instant dateCreation;
}