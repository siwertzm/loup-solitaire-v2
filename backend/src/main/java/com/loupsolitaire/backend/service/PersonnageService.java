package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ObjetDepart;

import lombok.RequiredArgsConstructor;

// Orchestre la creation de personnage telle que specifiee dans le parcours
// utilisateur : tirages de stats, disciplines choisies, arme de Maitrise des
// Armes (si applicable), equipement de depart fixe + aleatoire.
@Service
@RequiredArgsConstructor
public class PersonnageService {

    private static final int NB_DISCIPLINES_A_CHOISIR = 5;
    private static final int CHAPITRE_DEPART_ID = 0;

    // Table de tirage 0-9 pour l'objet de depart aleatoire (voir doc de
    // conception du parcours utilisateur).
    private static final Map<Integer, ObjetDepart> TABLE_OBJET_DEPART = Map.ofEntries(
            Map.entry(0, new ObjetDepart("glaive", 1)),
            Map.entry(1, new ObjetDepart("epee", 1)),
            Map.entry(2, new ObjetDepart("casque", 1)),
            Map.entry(3, new ObjetDepart("repas", 2)),
            Map.entry(4, new ObjetDepart("cotte_de_mailles", 1)),
            Map.entry(5, new ObjetDepart("masse", 1)),
            Map.entry(6, new ObjetDepart("potion_de_soin", 1)),
            Map.entry(7, new ObjetDepart("baton", 1)),
            Map.entry(8, new ObjetDepart("lance", 1)),
            Map.entry(9, new ObjetDepart("or", 12))
    );

    private final PersonnageRepository personnageRepository;
    private final DisciplineRepository disciplineRepository;
    private final ObjetRepository objetRepository;
    private final ChapitreRepository chapitreRepository;
    private final TableDeHasardService tableDeHasardService;
    private final InventaireService inventaireService;
    private final ConditionService conditionService;

    @Transactional
    public Personnage creerPersonnage(Utilisateur utilisateur, String nom, List<IdDiscipline> disciplinesChoisies) {
        List<Discipline> disciplines = resoudreDisciplines(disciplinesChoisies);

        Personnage personnage = new Personnage();
        personnage.setUtilisateur(utilisateur);
        personnage.setNom(nom);
        int habilite = 10 + tableDeHasardService.tirerChiffre();
        personnage.setHabiliteBase(habilite);
        // Valeur de depart avant equipement : ajustee automatiquement dans
        // equiperMateriel() des que la hache de depart est ajoutee (voir
        // InventaireService.recalculerHabiliteSiArme).
        personnage.setHabilite(habilite);

        int endurance = 20 + tableDeHasardService.tirerChiffre();
        personnage.setEnduranceMax(endurance);
        personnage.setEnduranceActuelle(endurance);

        personnage.setDisciplines(disciplines);
        personnage.setDateCreation(Instant.now());
        personnage.setChapitreActuel(recupererChapitreDepart());
        // Tirage fige des l'arrivee sur le chapitre de depart (voir
        // avancerVersChapitre pour la meme logique aux chapitres suivants).
        personnage.setDernierTirageHasard(tableDeHasardService.tirerChiffre());

        if (disciplinesChoisies.contains(IdDiscipline.MAITRISE_ARMES)) {
            personnage.setArmeMaitrisee(tirerArmeMaitrisee());
        }

        personnage = personnageRepository.save(personnage);

        equiperMateriel(personnage);

        return personnage;
    }

    private List<Discipline> resoudreDisciplines(List<IdDiscipline> disciplinesChoisies) {
        if (disciplinesChoisies == null
                || disciplinesChoisies.size() != NB_DISCIPLINES_A_CHOISIR
                || new HashSet<>(disciplinesChoisies).size() != NB_DISCIPLINES_A_CHOISIR) {
            throw new IllegalArgumentException(
                    "Il faut choisir exactement " + NB_DISCIPLINES_A_CHOISIR + " disciplines distinctes");
        }

        // ArrayList, PAS .toList() (immuable) : Hibernate doit pouvoir
        // vider/remplir cette collection lors d'un merge (ex. lors du
        // 2e save() lance par ObjetService apres l'ajout d'un objet), ce
        // qui echoue avec UnsupportedOperationException sur une liste
        // immuable.
        return new ArrayList<>(disciplinesChoisies.stream()
                .map(id -> disciplineRepository.findById(id)
                        .orElseThrow(() -> new RessourceNonTrouveeException("Discipline introuvable : " + id)))
                .toList());
    }

    private Chapitre recupererChapitreDepart() {
        return chapitreRepository.findById(CHAPITRE_DEPART_ID)
                .orElseThrow(() -> new RessourceNonTrouveeException(
                        "Chapitre de depart introuvable : " + CHAPITRE_DEPART_ID));
    }

    // Tirage uniforme parmi toutes les armes du catalogue.
    private Objet tirerArmeMaitrisee() {
        List<Objet> armes = objetRepository.findByCategorie(CategorieObjet.ARME);
        return tableDeHasardService.tirerParmi(armes);
    }

    private void equiperMateriel(Personnage personnage) {
        // Equipement fixe.
        ajouter(personnage, "hache", 1);
        ajouter(personnage, "repas", 1);
        ajouter(personnage, "carte", 1);

        // Or de depart : tirage unique, peut valoir 0 (rien a ajouter).
        int orDepart = tableDeHasardService.tirerChiffre();
        if (orDepart > 0) {
            ajouter(personnage, "or", orDepart);
        }

        // Objet de depart aleatoire (table fixe 0-9).
        ObjetDepart objetDepart = TABLE_OBJET_DEPART.get(tableDeHasardService.tirerChiffre());
        ajouter(personnage, objetDepart.objetId(), objetDepart.quantite());
    }

    private void ajouter(Personnage personnage, String objetId, int quantite) {
        Objet objet = objetRepository.findById(objetId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Objet introuvable : " + objetId));
        inventaireService.ajouterObjet(personnage, objet, quantite);
    }

    // A appeler a chaque fois que chapitreActuel change : l'HABILETE
    // temporaire (ex. essence d'Alether) ne vaut que pour le
    // chapitre/combat en cours.
    @Transactional
    public void reinitialiserHabiliteTemp(Personnage personnage) {
        if (personnage.getHabiliteTemp() != 0) {
            personnage.setHabiliteTemp(0);
            personnageRepository.save(personnage);
        }
    }

    // Deplace le personnage vers chapitreCibleId, s'il existe bien un Lien
    // valide (conditions comprises) depuis son chapitre actuel. Met a jour
    // chapitrePrecedent/chapitreActuel et reinitialise l'habilite temporaire.
    // Les effets/ennemis/objets du nouveau chapitre ne sont PAS appliques ici
    // (etape suivante).
    @Transactional
    public void avancerVersChapitre(Personnage personnage, Integer chapitreCibleId) {
        Integer chapitreActuelId = personnage.getChapitreActuel().getId();

        // personnage.getChapitreActuel() vient d'une session deja fermee
        // (chargee par le controleur) : ses collections lazy (liens) ne sont
        // pas accessibles telles quelles. On recharge le chapitre a neuf ici,
        // dans la transaction courante.
        Chapitre chapitreActuel = chapitreRepository.findById(chapitreActuelId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreActuelId));

        Lien lienChoisi = chapitreActuel.getLiens().stream()
                .filter(lien -> lien.getChapitreCible().getId().equals(chapitreCibleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun lien vers le chapitre " + chapitreCibleId + " depuis le chapitre " + chapitreActuelId));

        boolean conditionsRemplies = lienChoisi.getConditions().stream()
                .allMatch(cond -> conditionService.estDisponible(cond, personnage));
        if (!conditionsRemplies) {
            throw new IllegalArgumentException(
                    "Les conditions pour rejoindre le chapitre " + chapitreCibleId + " ne sont pas remplies");
        }

        personnage.setChapitrePrecedent(chapitreActuel);
        personnage.setChapitreActuel(lienChoisi.getChapitreCible());
        // Nouveau tirage FIGE des l'arrivee sur ce chapitre : ne doit plus
        // changer tant qu'on ne le quitte pas, meme si on recharge l'ecran
        // plusieurs fois (voir aussi creerPersonnage, meme logique au
        // premier chapitre).
        personnage.setDernierTirageHasard(tableDeHasardService.tirerChiffre());
        personnageRepository.save(personnage);

        reinitialiserHabiliteTemp(personnage);
    }
}