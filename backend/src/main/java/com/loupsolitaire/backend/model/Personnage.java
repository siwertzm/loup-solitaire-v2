package com.loupsolitaire.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.CollectionTable;   
import jakarta.persistence.ElementCollection; 
import jakarta.persistence.OrderColumn;       

import com.loupsolitaire.backend.model.enums.PorteeVol;

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

    // "Dernier personnage joué" cote accueil frontend (AccueilPage trie sur
    // ce champ pour placer ce personnage en premiere carte). Rafraichie
    // AUTOMATIQUEMENT a chaque modification de l'entite (avancer de
    // chapitre, tour de combat, ramasser un objet...) via @PreUpdate,
    // plutot que d'ajouter un appel explicite a chacun des tres nombreux
    // points du code qui font personnageRepository.save(personnage).
    // Initialisee a la creation (voir PersonnageService.creerPersonnage),
    // mais volontairement PAS nullable=false : les personnages crees avant
    // l'ajout de ce champ (ddl-auto=update ne backfill rien) l'auraient
    // sinon en violation du schema. Le tri cote controleur traite null
    // comme "jamais joue" (le plus ancien possible).
    private Instant derniereActivite;

    @PreUpdate
    private void surMiseAJour() {
        this.derniereActivite = Instant.now();
    }

    // Tirage 0-9 courant pour evaluer les conditions HASARD des liens.
    // Regenere a chaque chargement du chapitre (GET /chapitre), puis relu
    // (jamais re-tire) au moment de valider un choix (POST /chapitre/{id}) :
    // le joueur choisit en fonction de ce qu'il a vu affiche.
    private Integer dernierTirageHasard;

    // Non-null si un Effet VOL (valeur=1) attend que le joueur choisisse
    // quoi perdre. Voir EffetChapitreService.resoudreVolEnAttente et
    // PersonnageController POST /vol/{objetId}.
    @Enumerated(EnumType.STRING)
    private PorteeVol volEnAttente;

    // Dernier resultat de l'effet REPAS applique a l'arrivee sur le chapitre
    // (CHASSE, REPAS_CONSOMME, MALUS_ENDURANCE).
    @Enumerated(EnumType.STRING)
    private com.loupsolitaire.backend.model.enums.StatutRepas dernierStatutRepas;

    // true des que l'ENDURANCE tombe a 0 HORS combat (effet ENDURANCE ou
    // REPAS d'un chapitre, voir EffetChapitreService). Bloque alors toute
    // action jusqu'a resurrection (PersonnageService.ressusciter). La mort
    // EN COMBAT est geree separement (Combat.statut=DEFAITE, voir
    // PersonnageService.revenirApresDefaite) : ce champ ne s'applique pas
    // a ce cas-la.
    @Column(nullable = false)
    private boolean mort;

    // Journal du parcours : numeros des chapitres traverses, dans l'ordre
    // chronologique. Une valeur par ARRIVEE (chapitre de depart et revisites
    // compris : un retour apres une mort narrative ajoute le chapitre de
    // retour une 2e fois). @OrderColumn conserve l'ordre. Chargee a la
    // demande : a lire dans une transaction (open-in-view=false).
    @ElementCollection
    @CollectionTable(name = "personnage_chapitre_parcouru", joinColumns = @JoinColumn(name = "personnage_id"))
    @OrderColumn(name = "ordre")
    @Column(name = "chapitre_id", nullable = false)
    private List<Integer> chapitresParcourus = new ArrayList<>();

    // Journal : POSITIONS (dans chapitresParcourus) des arrivees ou le
    // personnage est mort. Sert a griser ces etapes dans le journal. La liste
    // des chapitres etant en ajout seul (jamais de suppression ni de
    // reordonnancement), une position identifie sans ambiguite UNE arrivee,
    // meme sur un chapitre traverse plusieurs fois.
    @ElementCollection
    @CollectionTable(name = "personnage_etape_mortelle", joinColumns = @JoinColumn(name = "personnage_id"))
    @Column(name = "ordre", nullable = false)
    private Set<Integer> etapesMortelles = new HashSet<>();

    // Point d'entree UNIQUE pour faire mourir le personnage, quelle que soit la
    // cause (chapitre de mort narrative, perte d'endurance, defaite en combat) :
    // positionne mort ET note dans le journal que la derniere arrivee est
    // celle ou il est mort. La mort survient toujours sur le chapitre courant,
    // c'est-a-dire la derniere arrivee du journal (les effets d'un chapitre sont
    // appliques juste apres son ajout au journal, voir
    // PersonnageService.avancerVersChapitre). Sans journal (personnage ancien),
    // seul mort est positionne.
    public void marquerMort() {
        this.mort = true;
        int derniereArrivee = chapitresParcourus.size() - 1;
        if (derniereArrivee >= 0) {
            etapesMortelles.add(derniereArrivee);
        }
    }
}