package com.loupsolitaire.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loupsolitaire.backend.exception.RessourceNonTrouveeException;
import com.loupsolitaire.backend.model.Chapitre;
import com.loupsolitaire.backend.model.Combat;
import com.loupsolitaire.backend.model.Discipline;
import com.loupsolitaire.backend.model.Lien;
import com.loupsolitaire.backend.model.Objet;
import com.loupsolitaire.backend.model.ObjetChap;
import com.loupsolitaire.backend.model.ObjetChapitreRamasse;
import com.loupsolitaire.backend.model.Personnage;
import com.loupsolitaire.backend.model.Utilisateur;
import com.loupsolitaire.backend.model.enums.CategorieObjet;
import com.loupsolitaire.backend.model.enums.IdDiscipline;
import com.loupsolitaire.backend.model.enums.StatutCombat;
import com.loupsolitaire.backend.model.enums.TypeEffet;
import com.loupsolitaire.backend.repository.ChapitreRepository;
import com.loupsolitaire.backend.repository.CombatRepository;
import com.loupsolitaire.backend.repository.DisciplineRepository;
import com.loupsolitaire.backend.repository.InventaireItemRepository;
import com.loupsolitaire.backend.repository.ObjetChapitreRamasseRepository;
import com.loupsolitaire.backend.repository.ObjetRepository;
import com.loupsolitaire.backend.repository.PersonnageRepository;
import com.loupsolitaire.backend.service.record.ObjetDepart;
import com.loupsolitaire.backend.service.record.ResultatAjout;

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
    private final InventaireItemRepository inventaireItemRepository;
    private final ObjetRepository objetRepository;
    private final ChapitreRepository chapitreRepository;
    private final TableDeHasardService tableDeHasardService;
    private final InventaireService inventaireService;
    private final ConditionService conditionService;
    private final EffetChapitreService effetChapitreService;
    private final CombatRepository combatRepository;
    private final ObjetChapitreRamasseRepository objetChapitreRamasseRepository;

    // Statuts de Combat qui autorisent a quitter un chapitre combat=true :
    // DEFAITE en est volontairement exclu (pas de flux "fin de partie"
    // construit ici, voir doc de conception - limitation connue) et
    // EN_COURS bien sur aussi (le combat n'est pas termine).
    private static final Set<StatutCombat> STATUTS_PERMETTANT_LA_SORTIE =
            Set.of(StatutCombat.VICTOIRE, StatutCombat.FUITE, StatutCombat.INTERROMPU);

    @Transactional
    public Personnage creerPersonnage(Utilisateur utilisateur, String nom, List<IdDiscipline> disciplinesChoisies, Integer hasardHabilite, Integer hasardEndurance) {
        List<Discipline> disciplines = resoudreDisciplines(disciplinesChoisies);

        Personnage personnage = new Personnage();
        personnage.setUtilisateur(utilisateur);
        personnage.setNom(nom);
        int habilite = 10 + (hasardHabilite);
        personnage.setHabiliteBase(habilite);
        // Valeur de depart avant equipement : ajustee automatiquement dans
        // equiperMateriel() des que la hache de depart est ajoutee (voir
        // InventaireService.recalculerHabiliteSiArme).
        personnage.setHabilite(habilite);

        int endurance = 20 + (hasardEndurance);
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

        // "coin" : monnaie premium (retour a un point de choix anterieur
        // apres une mort narrative, voir chapitre.json). Illimitee pour le
        // MVP1 (categorie OBJETS_SPECIAUX, non plafonnee) ; deviendra un
        // vrai achat en MVP2.
        ajouter(personnage, "coin", 999);

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
        verifierPasMort(personnage);
        if (personnage.getVolEnAttente() != null) {
            throw new IllegalArgumentException(
                    "Un vol est en attente de resolution (POST /vol/{objetId}) avant de pouvoir avancer");
        }

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

        // Filet de securite complementaire : bloque tout lien tant que le
        // combat n'est pas resolu d'une facon ou d'une autre (VICTOIRE/
        // FUITE/INTERROMPU), meme pour un type de condition qui n'aurait
        // aucun rapport avec le combat lui-meme. Le cas precis "quel lien
        // pour quelle issue" (victoire vs fuite vs arret force) est traite
        // par ConditionService.estLienDisponible juste en dessous.
        if (chapitreActuel.isCombat()) {
            Optional<Combat> combat = combatRepository
                    .findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, chapitreActuelId);
            boolean combatResolu = combat.isPresent() && STATUTS_PERMETTANT_LA_SORTIE.contains(combat.get().getStatut());
            if (!combatResolu) {
                throw new IllegalArgumentException(
                        "Le combat du chapitre " + chapitreActuelId + " n'est pas termine");
            }
        }

        boolean lienDisponible = conditionService.estLienDisponible(lienChoisi, personnage);
        if (!lienDisponible) {
            throw new IllegalArgumentException(
                    "Les conditions pour rejoindre le chapitre " + chapitreCibleId + " ne sont pas remplies");
        }

        personnage.setChapitrePrecedent(chapitreActuel);
        Chapitre nouveauChapitre = lienChoisi.getChapitreCible();
        personnage.setChapitreActuel(nouveauChapitre);

        // Si ce chapitre est un combat DEJA resolu (VICTOIRE/DEFAITE/FUITE/
        // INTERROMPU) pour ce personnage, on le supprime pour permettre un
        // combat entierement neuf : sans ca, CombatService.
        // combatEnCoursOuNouveau retrouverait l'ancien Combat et renverrait
        // son issue figee au lieu d'un vrai nouvel affrontement. Necessaire
        // pour les retours en arriere payants qui retraversent un chapitre
        // de combat (ex. chapitre 78 -> 220, chapitre 299 -> 227). Un
        // Combat encore EN_COURS n'est jamais supprime (ne devrait de toute
        // facon pas arriver, avancerVersChapitre bloque deja la sortie d'un
        // combat non resolu).
        if (nouveauChapitre.isCombat()) {
            combatRepository
                    .findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, nouveauChapitre.getId())
                    .filter(combat -> combat.getStatut() != StatutCombat.EN_COURS)
                    .ifPresent(combatRepository::delete);
        }

        // Nouveau tirage FIGE des l'arrivee sur ce chapitre : ne doit plus
        // changer tant qu'on ne le quitte pas, meme si on recharge l'ecran
        // plusieurs fois (voir aussi creerPersonnage, meme logique au
        // premier chapitre).
        personnage.setDernierTirageHasard(tableDeHasardService.tirerChiffre());
        personnageRepository.save(personnage);

        reinitialiserHabiliteTemp(personnage);
        appliquerGuerison(personnage, nouveauChapitre);

        // Effets du nouveau chapitre : appliques UNE SEULE FOIS ici, a
        // l'arrivee (pas a chaque GET /chapitre).
        boolean aUnEffetRepas = nouveauChapitre.getEffets().stream()
                .anyMatch(effet -> effet.getType() == TypeEffet.REPAS);
        if (aUnEffetRepas) {
            effetChapitreService.appliquerEffetRepas(personnage);
        }

        nouveauChapitre.getEffets().stream()
                .filter(effet -> effet.getType() == TypeEffet.HABILETE)
                .forEach(effet -> effetChapitreService.appliquerEffetHabilite(personnage, effet));

        nouveauChapitre.getEffets().stream()
                .filter(effet -> effet.getType() == TypeEffet.ENDURANCE)
                .forEach(effet -> effetChapitreService.appliquerEffetEndurance(personnage, effet));

        nouveauChapitre.getEffets().stream()
                .filter(effet -> effet.getType() == TypeEffet.VOL)
                .forEach(effet -> effetChapitreService.appliquerEffetVol(personnage, effet));

        // Objets du chapitre : seuls les OBLIGATOIRES (optionnel=false)
        // sont appliques automatiquement ici (positif = ajout, negatif =
        // paiement/destruction, ex. chapitres 246/262/236). Les objets
        // optionnels restent visibles via GET /chapitre et se ramassent
        // manuellement via POST /objets/{objetId}, deja existant.
        nouveauChapitre.getObjets().stream()
                .filter(objetChap -> !objetChap.isOptionnel())
                .forEach(objetChap -> appliquerObjetChap(personnage, nouveauChapitre, objetChap));
    }

    // MVP1 : apres une DEFAITE en combat, le joueur peut payer 1 coin pour
    // revenir au chapitre precedent, endurance entierement restauree. Pas
    // de calcul de dominateur ici (contrairement aux 16 morts narratives) :
    // c'est un simple retour d'un cran, meme s'il ne mene pas forcement a
    // un autre choix que ce meme combat (voir doc de conception, choix
    // volontairement simplifie). Si le joueur revient plus tard sur ce
    // meme chapitre de combat, avancerVersChapitre le reinitialise deja
    // automatiquement (Combat resolu supprime a l'arrivee).
    @Transactional
    public void revenirApresDefaite(Personnage personnage) {
        Integer chapitreActuelId = personnage.getChapitreActuel().getId();
        Combat combat = combatRepository
                .findFirstByPersonnageAndChapitreIdOrderByCreeLeDesc(personnage, chapitreActuelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun combat trouve sur le chapitre " + chapitreActuelId));

        if (combat.getStatut() != StatutCombat.DEFAITE) {
            throw new IllegalArgumentException(
                    "Ce combat n'est pas perdu (statut actuel : " + combat.getStatut() + ")");
        }

        Chapitre precedent = personnage.getChapitrePrecedent();
        if (precedent == null) {
            throw new IllegalArgumentException("Aucun chapitre precedent connu pour ce personnage");
        }

        boolean possedeCoin = inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> item.getObjet().getId().equals("coin") && item.getQuantite() >= 1);
        if (!possedeCoin) {
            throw new IllegalArgumentException("Vous n'avez pas de Piece Premium (coin) pour revenir en arriere");
        }
        // MVP1 : coin illimite, on ne le retire pas (coherent avec les
        // retours narratifs via chapitre.json, qui ne consomment rien non
        // plus). A revoir pour le MVP2 si le coin devient une vraie
        // ressource limitee/achetee.

        personnage.setChapitreActuel(precedent);
        personnage.setEnduranceActuelle(personnage.getEnduranceMax());
        // Nouveau tirage FIGE, comme pour tout changement de chapitre.
        personnage.setDernierTirageHasard(tableDeHasardService.tirerChiffre());
        personnageRepository.save(personnage);

        reinitialiserHabiliteTemp(personnage);
    }

    // Garde reutilisee par toute action mutant l'etat du personnage : un
    // personnage mort (perte d'endurance HORS combat, voir Personnage.mort)
    // ne peut plus rien faire tant qu'il n'a pas ete ressuscite. La mort EN
    // COMBAT n'est PAS concernee ici (voir revenirApresDefaite) : ce n'est
    // pas ce champ qui bloque alors, mais Combat.statut lui-meme.
    private void verifierPasMort(Personnage personnage) {
        if (personnage.isMort()) {
            throw new IllegalArgumentException(
                    "Ce personnage est mort (perte d'endurance) : ressuscitez-le via "
                            + "POST /personnages/{id}/ressusciter avant de continuer");
        }
    }

    // MVP1 : seule action possible pour un personnage mort (perte
    // d'endurance HORS combat) - paye 1 coin (non retire, illimite comme
    // pour revenirApresDefaite) pour restaurer l'endurance a fond et
    // repasser mort a false. Reste sur place : ne change PAS de chapitre,
    // contrairement a revenirApresDefaite.
    @Transactional
    public void ressusciter(Personnage personnage) {
        if (!personnage.isMort()) {
            throw new IllegalArgumentException("Ce personnage n'est pas mort");
        }

        boolean possedeCoin = inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> item.getObjet().getId().equals("coin") && item.getQuantite() >= 1);
        if (!possedeCoin) {
            throw new IllegalArgumentException("Vous n'avez pas de Piece Premium (coin) pour ressusciter");
        }

        personnage.setEnduranceActuelle(personnage.getEnduranceMax());
        personnage.setMort(false);
        personnageRepository.save(personnage);
    }

    // Discipline Kai Guerison : +1 point d'ENDURANCE (plafonne a
    // enduranceMax) a chaque fois qu'on arrive sur un nouveau chapitre
    // SANS combat. Pas de recuperation sur un chapitre combat=true, meme
    // si le combat n'est pas encore engage (l'idee du livre est "un
    // paragraphe traverse sans avoir a se battre", pas juste "pas encore
    // combattu").
    private void appliquerGuerison(Personnage personnage, Chapitre nouveauChapitre) {
        if (nouveauChapitre.isCombat()) {
            return;
        }

        boolean possedeGuerison = personnage.getDisciplines().stream()
                .anyMatch(discipline -> discipline.getId() == IdDiscipline.GUERISON);
        if (!possedeGuerison) {
            return;
        }

        int nouvelleEndurance = Math.min(personnage.getEnduranceMax(), personnage.getEnduranceActuelle() + 1);
        personnage.setEnduranceActuelle(nouvelleEndurance);
        personnageRepository.save(personnage);
    }

    private void appliquerObjetChap(Personnage personnage, Chapitre chapitre, ObjetChap objetChap) {
        int valeur = objetChap.getValeur();
        if (valeur > 0) {
            ResultatAjout resultat = inventaireService.ajouterObjet(personnage, objetChap.getObjet(), valeur);

            if (!objetChap.isOptionnel()) {
                // Trace ce qui a REELLEMENT ete applique (peut etre < valeur si
                // la categorie etait deja pleine a l'arrivee) : necessaire pour
                // que ChapitreMapper puisse annoncer le manque au frontend, et
                // pour que le joueur puisse le completer plus tard (voir
                // ramasserObjetDuChapitre) une fois de la place liberee.
                //
                // Reecrit a chaque nouvelle arrivee sur ce chapitre (pas
                // cumule entre plusieurs visites) : coherent avec le
                // comportement existant "applique a l'arrivee, pas de memoire
                // long terme entre deux passages sur le meme paragraphe".
                Optional<ObjetChapitreRamasse> existant = objetChapitreRamasseRepository
                        .findByPersonnageIdAndChapitreIdAndObjetId(
                                personnage.getId(), chapitre.getId(), objetChap.getObjet().getId());

                ObjetChapitreRamasse ramasse = existant.orElseGet(() -> {
                    ObjetChapitreRamasse nouveau = new ObjetChapitreRamasse();
                    nouveau.setPersonnage(personnage);
                    nouveau.setChapitre(chapitre);
                    nouveau.setObjet(objetChap.getObjet());
                    return nouveau;
                });
                ramasse.setQuantite(resultat.quantiteAjoutee());
                objetChapitreRamasseRepository.save(ramasse);
            }
        } else if (valeur < 0) {
            // Le Lien menant a ce chapitre garantit deja la possession
            // suffisante (condition bourse/objet), mais retirerObjet
            // revalide quand meme (voir InventaireService).
            inventaireService.retirerObjet(personnage, objetChap.getObjet(), Math.abs(valeur));
        }
    }

    // Ramassage MANUEL (POST /objets/{objetId}) :
    // - objet OPTIONNEL : ajoute 1 exemplaire par appel (comportement
    //   historique).
    // - objet OBLIGATOIRE avec un manque trace (voir appliquerObjetChap) :
    //   complete ce qui n'avait pas pu etre applique automatiquement a
    //   l'arrivee (categorie pleine a ce moment-la). Le frontend propose ce
    //   bouton uniquement quand GET /chapitre annonce un reste > 0 sur un
    //   objet obligatoire.
    //
    // Securise dans les deux cas contre un joueur qui tenterait de se
    // donner n'importe quel objet du catalogue - verifie que l'objet est
    // bien propose par le chapitre ACTUEL.
    @Transactional
    public void ramasserObjetDuChapitre(Personnage personnage, Objet objet) {
        verifierPasMort(personnage);
        Integer chapitreActuelId = personnage.getChapitreActuel().getId();
        Chapitre chapitreActuel = chapitreRepository.findById(chapitreActuelId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreActuelId));

        ObjetChap objetChap = chapitreActuel.getObjets().stream()
                .filter(oc -> oc.getObjet().getId().equals(objet.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cet objet n'est pas disponible sur le chapitre " + chapitreActuelId));

        // Plafond reel : sans ce suivi persistant, rien n'empeche de reprendre
        // le meme objet a l'infini sur le meme chapitre (voir ObjetChapitreRamasse).
        Optional<ObjetChapitreRamasse> existant = objetChapitreRamasseRepository
                .findByPersonnageIdAndChapitreIdAndObjetId(personnage.getId(), chapitreActuelId, objet.getId());
        int dejaApplique = existant.map(ObjetChapitreRamasse::getQuantite).orElse(0);

        if (dejaApplique >= objetChap.getValeur()) {
            throw new IllegalArgumentException(
                    "Vous avez deja ramasse tous les exemplaires de " + objet.getId()
                            + " disponibles sur ce chapitre");
        }

        // Optionnel : 1 a la fois (ecran de ramassage, "PRENDRE" repete).
        // Obligatoire : complete tout le manque en un appel (ecran "PLEIN",
        // un seul clic une fois la place liberee).
        int quantiteDemandee = objetChap.isOptionnel() ? 1 : objetChap.getValeur() - dejaApplique;

        ResultatAjout resultat = inventaireService.ajouterObjet(personnage, objet, quantiteDemandee);

        ObjetChapitreRamasse ramasse = existant.orElseGet(() -> {
            ObjetChapitreRamasse nouveau = new ObjetChapitreRamasse();
            nouveau.setPersonnage(personnage);
            nouveau.setChapitre(chapitreActuel);
            nouveau.setObjet(objet);
            nouveau.setQuantite(0);
            return nouveau;
        });
        ramasse.setQuantite(ramasse.getQuantite() + resultat.quantiteAjoutee());
        objetChapitreRamasseRepository.save(ramasse);
    }

    // Echange volontaire (ex. chapitre 307 : le Marteau de Guerre de
    // l'ermite contre une arme deja possedee). Valide que le chapitre
    // ACTUEL du personnage propose bien cet echange precis (Effet ECHANGE
    // dont une condition cible objetAAjouter), pour empecher un joueur
    // d'echanger n'importe quelle arme a n'importe quel chapitre.
    @Transactional
    public void echangerObjet(Personnage personnage, Objet objetARetirer, Objet objetAAjouter) {
        verifierPasMort(personnage);
        if (objetARetirer.getCategorie() != objetAAjouter.getCategorie()) {
            throw new IllegalArgumentException(
                    "Impossible d'echanger des objets de categories differentes ("
                            + objetARetirer.getCategorie() + " contre " + objetAAjouter.getCategorie() + ")");
        }

        Integer chapitreActuelId = personnage.getChapitreActuel().getId();
        Chapitre chapitreActuel = chapitreRepository.findById(chapitreActuelId)
                .orElseThrow(() -> new RessourceNonTrouveeException("Chapitre introuvable : " + chapitreActuelId));

        boolean echangeAutorise = chapitreActuel.getEffets().stream()
                .filter(effet -> effet.getType() == TypeEffet.ECHANGE)
                .flatMap(effet -> effet.getConditions().stream())
                .anyMatch(cond -> objetAAjouter.getId().equals(cond.getTargetId()));

        if (!echangeAutorise) {
            throw new IllegalArgumentException(
                    "Aucun echange pour " + objetAAjouter.getId() + " n'est propose sur ce chapitre");
        }

        boolean possede = inventaireService.listerInventaire(personnage).stream()
                .anyMatch(item -> item.getObjet().getId().equals(objetARetirer.getId()) && item.getQuantite() > 0);
        if (!possede) {
            throw new IllegalArgumentException(
                    "Vous ne possedez pas " + objetARetirer.getId() + ", impossible de l'echanger");
        }

        inventaireService.remplacerObjet(personnage, objetARetirer, 1, objetAAjouter, 1);
    }

    @Transactional
    public void supprimerPersonnage(Personnage personnage) {
        inventaireItemRepository.deleteByPersonnageId(personnage.getId());
        objetChapitreRamasseRepository.deleteByPersonnageId(personnage.getId());
        combatRepository.deleteAll(combatRepository.findByPersonnage(personnage));
        personnageRepository.delete(personnage);
    }
}