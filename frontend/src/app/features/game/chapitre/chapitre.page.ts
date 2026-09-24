import {
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { PersonnageResume } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import {
  ChapitreResponse,
  EnnemiChapitreResponse,
  LienResponse,
} from '../../../core/models/chapitre.model';
import { ChapitreService } from '../../../core/services/chapitre.service';
import { CombatService } from '../../../core/services/combat.service';
import { DisciplineResume } from '../../../core/models/personnage.model';
import { DisciplineService } from '../../../core/services/discipline.service';
import { ObjetResume } from '../../../core/models/personnage.model';
import { ObjetService } from '../../../core/services/objet.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { ChapitreObjetsComponent } from './objets/chapitre-objets.component';
import { ChapitreEffetsComponent } from './effets/chapitre-effets.component';
import { NavBarComponent } from '../../shared/nav-bar/nav-bar.component';

/**
 * Écran central du jeu : affiche le chapitre en cours et la fiche du personnage.
 *
 * La liste des objets du chapitre + le résumé d'inventaire ("sac") sont
 * délégués à <app-chapitre-objets> (dossier objets/) pour garder ce fichier
 * gérable — ce composant reste seul propriétaire des données (personnage,
 * chapitre, catalogues disciplines/objets) et de la navigation.
 */
@Component({
  selector: 'app-chapitre',
  standalone: true,
  imports: [
    IonContent,
    RouterLink,
    ChapitreObjetsComponent,
    ChapitreEffetsComponent,
    NavBarComponent,
    TranslatePipe,
  ],
  templateUrl: './chapitre.page.html',
  styleUrl: './chapitre.page.scss',
})
export class ChapitrePage implements OnInit, ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chapitreService = inject(ChapitreService);
  private readonly combatService = inject(CombatService);
  private readonly personnageService = inject(PersonnageService);
  private readonly disciplineService = inject(DisciplineService);
  private readonly objetService = inject(ObjetService);
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly translate = inject(TranslateService);
  private readonly zoneTexte = viewChild<ElementRef<HTMLElement>>('zoneTexte');

  private dernierChapitreScrollId: number | null = null;

  readonly personnageId = signal<string | null>(null);

  // Signal stockant les infos du personnage
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly initiale = computed(
    () =>
      this.nomPersonnage().trim().charAt(0).toUpperCase() ||
      this.translate.instant('CHAPITRE_PAGE.INITIALE_PAR_DEFAUT'),
  );

  // Signal stockant tous les objets disponibles (catalogue, pour nomObjet()
  // et transmis à <app-chapitre-objets> pour la résolution des icônes)
  readonly tousObjets = signal<ObjetResume[]>([]);

  // Signal stockant le chapitre courant
  readonly chapitre = signal<ChapitreResponse | null>(null);

  readonly numeroChapitreAffiche = computed(() => {
    const id = this.chapitre()?.id;
    if (id === 2101 || id === 2102) {
      return 21;
    }
    return id;
  });

  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);

  // Signal stockant toutes les disciplines disponibles
  readonly toutesDisciplines = signal<DisciplineResume[]>([]);

  // Computed properties pour accéder facilement aux infos du personnage
  readonly nomPersonnage = computed(() => this.personnage()?.nom ?? '');

  readonly habilite = computed(
    () => (this.personnage()?.habilite ?? 0) + (this.personnage()?.habiliteTemp ?? 0),
  );
  readonly enduranceActuelle = computed(() => this.personnage()?.enduranceActuelle ?? 0);

  // Pourcentages pour le remplissage visuel des barres
  readonly endurancePourcentage = computed(() => {
    const max = this.enduranceMax();
    return Math.min(100, Math.max(0, (this.enduranceActuelle() / max) * 100));
  });

  readonly habilitePourcentage = computed(() => {
    const max = this.habilite(); // Valeur d'Habilite maximale estimée
    return Math.min(100, Math.max(0, (this.habilite() / max) * 100));
  });

  readonly enduranceMax = computed(() => this.personnage()?.enduranceMax ?? 0);
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);

  // Computed properties du chapitre
  readonly liens = computed(() => this.chapitre()?.liens ?? []);
  readonly objets = computed(() => this.chapitre()?.objets ?? []);
  readonly ennemis = computed(() => this.chapitre()?.ennemis ?? []);
  readonly effets = computed(() => this.chapitre()?.effets ?? []);
  readonly estCombat = computed(() => this.chapitre()?.combat ?? false);

  // Quantité restante à prendre par objet du chapitre (objetId -> restant),
  // partagée en liaison bidirectionnelle avec <app-chapitre-objets>
  // ([(restants)]) qui la décrémente à chaque ramassage. Initialisée à
  // chaque GET /chapitre (le backend y renvoie déjà "déclaré - déjà pris",
  // voir ChapitreMapper). Vit ici et pas dans le composant enfant, qui
  // n'est monté que tant que l'onglet "objets" est ouvert.
  readonly restantsObjets = signal<Record<string, number>>({});

  // Objets (valeur > 0 : un gain, pas un paiement) qu'il reste encore à
  // prendre sur ce chapitre — optionnels non ramassés, ou obligatoires
  // incomplets (catégorie pleine à l'arrivée). Non vide = confirmation
  // demandée avant de quitter le chapitre (voir choisirLien).
  readonly objetsNonPris = computed(() =>
    this.objets()
      .filter((o) => o.valeur > 0)
      .map((o) => ({ ...o, restant: this.restantsObjets()[o.objetId] ?? 0 }))
      .filter((o) => o.restant > 0),
  );

  // Lien choisi par le joueur alors que des objets restaient à prendre :
  // non null = popup de confirmation affiché, en attente de sa réponse.
  readonly lienEnAttenteConfirmation = signal<LienResponse | null>(null);

  // Jeton repere dans le champ "text" (voir chapitre.json) : indique
  // l'endroit ou inserer une carte ennemi (visuelle, pas juste textuelle)
  // au milieu du recit. Un vrai composant/element structure ne peut pas
  // etre injecte via [innerHTML] (le html reste alors de la donnee, pas du
  // Angular compile) : on decoupe donc le texte en segments autour de ce
  // jeton, et on alterne blocs de texte / carte ennemi au rendu (voir
  // .texte-contenu et .carte-ennemi dans le html + scss). Les occurrences
  // du jeton sont associees aux ennemis du chapitre dans l'ordre
  // d'apparition (1er jeton -> chapitre().ennemis[0], etc.).
  private readonly jetonCarteEnnemi = '[[ENNEMI]]';

  readonly segmentsTexte = computed<
    Array<{ type: 'texte'; html: string } | { type: 'ennemi'; ennemi: EnnemiChapitreResponse }>
  >(() => {
    const texte = this.chapitre()?.text ?? '';
    const ennemis = this.ennemis();
    const morceaux = texte.split(this.jetonCarteEnnemi);

    const segments: Array<
      { type: 'texte'; html: string } | { type: 'ennemi'; ennemi: EnnemiChapitreResponse }
    > = [];
    morceaux.forEach((morceau, index) => {
      const trimme = morceau.trim();
      if (trimme.length > 0) {
        segments.push({ type: 'texte', html: trimme });
      }
      // Un jeton se trouve juste apres ce morceau, sauf pour le dernier.
      if (index < morceaux.length - 1 && ennemis[index]) {
        segments.push({ type: 'ennemi', ennemi: ennemis[index] });
      }
    });
    return segments;
  });

  readonly combatDisponible = signal(false);
  // Mort HORS combat (voir plus bas) et mort EN combat (Combat.statut =
  // DEFAITE, voir CombatService.appliquerDegatsAuJoueur côté backend) sont
  // toutes deux reflétées par le même Personnage.mort — mais leur remède
  // diffère : ressusciter() (rester sur ce chapitre) échoue volontairement
  // côté backend si une DEFAITE est en attente (voir
  // PersonnageService.ressusciter) puisque cette mort-là exige de reculer
  // au chapitre précédent via CombatPage.terminer() ->
  // revenirApresDefaite(). Ce signal permet donc au pied de page de
  // proposer le bon bouton plutôt que de laisser ressusciter() échouer.
  readonly combatEnDefaite = signal(false);

  // Un vol en attente (voir <app-chapitre-effets>) bloque avancerVersChapitre
  // côté backend (400 : "Un vol est en attente de resolution"). Tant qu'il
  // n'est pas résolu, on remplace les liens du pied de page par un bouton
  // "CHOISIR" qui renvoie vers l'onglet Effets, où se trouve déjà le popup
  // de résolution (voir ChapitreEffetsComponent.ouvrirChoixVol).
  readonly volEnAttente = computed(() => this.personnage()?.volEnAttente ?? null);

  // Mort HORS combat (chapitre de mort narrative, ou perte d'endurance via
  // un effet/repas de chapitre — voir Personnage.mort). Prend le pas sur
  // tout le reste du pied de page (liens, vol en attente, combat) : le
  // backend refuse toute action tant que le personnage n'a pas été
  // ressuscité (voir PersonnageService.verifierPasMort côté backend).
  readonly mort = computed(() => this.personnage()?.mort ?? false);
  readonly ressusciterEnCours = signal(false);
  readonly erreurRessusciter = signal<string | null>(null);

  /**
   * Lien "retour" propre aux chapitres de mort NARRATIVE (ex. 53 -> 47,
   * 108 -> 129...) : dans les données du livre, ces chapitres ne gardent
   * qu'un seul lien restant après filtrage de la page de fin de partie,
   * conditionné par la possession d'une Pièce Premium ("coin"). C'est ce
   * lien qu'il faut suivre après ressusciter(), pas rester sur place.
   *
   * Les chapitres où la mort vient d'une perte d'endurance (effet/repas
   * sur un chapitre "normal") n'ont pas ce genre de lien : le fallback
   * (rester sur le même chapitre) s'applique alors.
   */
  private trouverLienRetourNarratif(): LienResponse | null {
    return (
      this.liens().find(
        (lien) => lien.conditions[0]?.type === 'OBJET' && lien.conditions[0]?.targetId === 'coin',
      ) ?? null
    );
  }

  /**
   * Mort EN combat (combatEnDefaite()) : ressusciter() échouerait
   * volontairement côté backend (voir PersonnageService.ressusciter). Le
   * remède est revenirApresDefaite() (même appel que CombatPage.terminer()
   * sur DEFAITE), qui recule au chapitre précédent ET remet mort=false —
   * on recharge donc ensuite le chapitre en place, sans navigation.
   */
  revenirApresDefaite(): void {
    const id = this.personnageId();
    if (!id || this.ressusciterEnCours()) return;

    this.ressusciterEnCours.set(true);
    this.erreurRessusciter.set(null);
    this.personnageService.revenirApresDefaite(id).subscribe({
      next: () => {
        this.ressusciterEnCours.set(false);
        this.chargerToutesLesDonnees();
      },
      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_RETOUR_DEFAITE'), err);
        this.ressusciterEnCours.set(false);
        this.erreurRessusciter.set(
          err?.error?.message ?? this.translate.instant('CHAPITRE_PAGE.ERREUR_REVENIR_INSTANT'),
        );
      },
    });
  }

  /**
   * POST /personnages/{id}/ressusciter.
   * - Mort narrative (lien de retour détecté) : on avance ensuite vers ce
   *   lien précis, comme le ferait le livre papier.
   * - Mort par perte d'endurance (pas de lien de retour) : on recharge
   *   simplement le chapitre courant, sans en changer.
   */
  ressusciter(): void {
    const id = this.personnageId();
    if (!id || this.ressusciterEnCours()) return;

    const lienRetour = this.trouverLienRetourNarratif();

    this.ressusciterEnCours.set(true);
    this.erreurRessusciter.set(null);
    this.personnageService.ressusciter(id).subscribe({
      next: () => {
        if (!lienRetour) {
          this.ressusciterEnCours.set(false);
          this.chargerToutesLesDonnees();
          return;
        }

        this.chapitreService.avancerVersChapitre(id, lienRetour.chapitreCibleId).subscribe({
          next: () => {
            this.ressusciterEnCours.set(false);
            this.chargerToutesLesDonnees();
          },
          error: (err) => {
            console.error(
              this.translate.instant('CHAPITRE_PAGE.ERREUR_AVANCEMENT_RESURRECTION'),
              err,
            );
            this.ressusciterEnCours.set(false);
            this.erreurRessusciter.set(
              this.translate.instant('CHAPITRE_PAGE.ERREUR_AVANCER_RESURRECTION'),
            );
          },
        });
      },
      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_RESURRECTION'), err);
        this.ressusciterEnCours.set(false);
        this.erreurRessusciter.set(
          err?.error?.message ?? this.translate.instant('CHAPITRE_PAGE.ERREUR_RESSUSCITER_INSTANT'),
        );
      },
    });
  }

  /** Ouvre (sans le refermer si déjà ouvert) l'onglet Effets pour que le
   * joueur résolve son vol en attente — pas de toggle ici, contrairement à
   * selectionnerOnglet(), pour garantir que l'onglet s'affiche à coup sûr. */
  allerChoisirVol(): void {
    this.ongletActif.set('effets');
    this.ongletLeve.set('effets');
    setTimeout(() => this.ongletLeve.set(null), 300);
  }

  readonly ongletActif = signal<'chapitre' | 'combat' | 'objets' | 'effets'>('chapitre');
  readonly ongletLeve = signal<'chapitre' | 'combat' | 'objets' | 'effets' | null>(null);
  readonly ongletObjetsClique = signal(false);
  private readonly chapitreObjetCliqueKey = 'loup-solitaire:chapitre-objet-clique';
  private readonly chapitreHasardTermineKey = 'loup-solitaire:chapitre-hasard-termine';

  // Signaux pour gérer le hasard (révélation et roulement)
  readonly hasardRoule = signal(false);
  readonly hasardResultatVisible = signal(false);
  readonly hasardTermine = signal(false);
  readonly faceHasard = signal<number | string>('?');

  readonly ancienTirageHasard = signal<number | null>(null);
  private readonly ancienTirageHasardKey = 'loup-solitaire:ancien-tirage-chapitre-21';

  // Endurance que le joueur avait avant d'arriver au 2102.
  readonly enduranceAvant2102 = signal<number | null>(null);

  private readonly enduranceAvant2102Key = 'loup-solitaire:endurance-avant-2102';

  readonly mortAffichee = computed(() => {
    // Au 2102, tant que le dernier dé n'a pas été révélé,
    // on ne montre pas encore visuellement la mort.
    if (this.chapitre()?.id === 2102 && !this.hasardTermine()) {
      return false;
    }

    return this.mort();
  });

  readonly enduranceAffichee = computed(() => {
    // Même principe pour la jauge : on garde l'endurance
    // que le joueur avait avant son dernier tirage.
    if (this.chapitre()?.id === 2102 && !this.hasardTermine()) {
      return this.enduranceAvant2102() ?? this.enduranceActuelle();
    }

    return this.enduranceActuelle();
  });

  readonly endurancePourcentageAffichee = computed(() => {
    const max = this.enduranceMax();

    if (max <= 0) {
      return 0;
    }

    return Math.min(100, Math.max(0, (this.enduranceAffichee() / max) * 100));
  });

  private enregistrerEnduranceAvant2102(): void {
    const personnageId = this.personnageId();
    const endurance = this.personnage()?.enduranceActuelle;

    if (!personnageId || endurance === null || endurance === undefined) {
      return;
    }

    this.enduranceAvant2102.set(endurance);

    localStorage.setItem(`${this.enduranceAvant2102Key}:${personnageId}`, String(endurance));
  }

  private chargerEnduranceAvant2102(): void {
    const personnageId = this.personnageId();

    if (!personnageId) {
      return;
    }

    const valeur = localStorage.getItem(`${this.enduranceAvant2102Key}:${personnageId}`);

    this.enduranceAvant2102.set(valeur !== null ? Number(valeur) : null);
  }

  readonly aConditionHasard = computed(
    () =>
      this.liens().some((lien) =>
        lien.conditions.some((condition) => condition.type === 'HASARD'),
      ) ||
      this.effets().some((effet) =>
        effet.conditions.some((condition) => condition.type === 'HASARD'),
      ),
  );

  revelerHasard(): void {
    if (this.hasardRoule() || this.hasardResultatVisible() || this.hasardTermine()) {
      return;
    }

    const valeur = this.chapitre()?.tirageHasard;

    if (valeur === null || valeur === undefined) {
      return;
    }

    // 1. Le dé roule
    this.hasardRoule.set(true);

    setTimeout(() => {
      // 2. Le dé s'arrête sur la vraie valeur
      this.faceHasard.set(valeur);
      this.hasardRoule.set(false);
      this.hasardResultatVisible.set(true);

      // 3. On laisse le résultat affiché 2 secondes
      setTimeout(() => {
        this.hasardResultatVisible.set(false);
        this.hasardTermine.set(true);
        this.enregistrerChapitreHasardTermine();
        if (this.redirigerEchecChapitre21()) {
          return;
        }
      }, 2000);
    }, 640);
  }

  private redirigerEchecChapitre21(): boolean {
    const chapitreId = this.chapitre()?.id;
    let chapitreCible: number | null = null;

    if (chapitreId === 21) {
      chapitreCible = 2101;
    } else if (chapitreId === 2101) {
      chapitreCible = 2102;
    }

    if (chapitreCible === null) {
      return false;
    }

    const lien = this.liens().find(
      (lien) => lien.chapitreCibleId === chapitreCible && lien.disponible,
    );

    if (!lien) {
      return false;
    }
    this.enregistrerAncienTirageHasard();

    if (chapitreId === 2101 && chapitreCible === 2102) {
      this.enregistrerEnduranceAvant2102();
    }

    // Redirection automatique (échec au hasard), pas un choix du joueur :
    // pas de confirmation "objets non pris".
    this.choisirLien(lien, true);
    return true;
  }

  private enregistrerAncienTirageHasard(): void {
    const valeur = this.chapitre()?.tirageHasard;

    if (valeur === null || valeur === undefined) {
      return;
    }

    this.ancienTirageHasard.set(valeur);

    localStorage.setItem(this.ancienTirageHasardKey, String(valeur));
  }

  selectionnerOnglet(onglet: 'chapitre' | 'combat' | 'objets' | 'effets'): void {
    if (onglet === 'objets') {
      this.ongletObjetsClique.set(true);
      this.enregistrerChapitreObjetClique();
    }

    // Si on reclique sur l'onglet déjà ouvert, on ferme l'encart
    if (this.ongletActif() === onglet && onglet !== 'chapitre') {
      this.ongletActif.set('chapitre');
    } else {
      this.ongletActif.set(onglet);
    }

    this.ongletLeve.set(onglet);

    setTimeout(() => {
      this.ongletLeve.set(null);
    }, 300);
  }

  constructor() {
    // La feuille "SAC À DOS" (globale, montée à la racine) garde sa propre
    // copie de la fiche personnage : un ramassage/retrait fait depuis elle
    // ne met pas à jour automatiquement celle de cette page. On synchronise
    // ici dès que la feuille notifie une mise à jour pour CE personnage.
    effect(() => {
      const nouveau = this.inventaireSheet.personnageMisAJour();
      if (nouveau && nouveau.id === this.personnageId()) {
        this.personnage.set(nouveau);
      }
    });

    effect(() => {
      const chapitreId = this.chapitre()?.id ?? null;
      const zone = this.zoneTexte();

      if (!zone || chapitreId === null) {
        return;
      }

      if (this.dernierChapitreScrollId === chapitreId) {
        return;
      }

      this.dernierChapitreScrollId = chapitreId;

      requestAnimationFrame(() => {
        zone.nativeElement.scrollTo({
          top: 0,
          behavior: 'auto',
        });
      });
    });
  }

  ngOnInit(): void {
    this.initialiserId();
  }

  ionViewWillEnter(): void {
    this.initialiserId();
    if (this.personnageId()) {
      this.chargerToutesLesDonnees();
    }
  }

  private initialiserId(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.personnageId.set(id);
    } else {
      this.erreur.set(this.translate.instant('CHAPITRE_PAGE.ERREUR_IDENTIFIANT_INTROUVABLE'));
      this.chargement.set(false);
    }
  }

  /**
   * Récupère à la fois les informations du personnage et du chapitre courant.
   */
  chargerToutesLesDonnees(): void {
    const id = this.personnageId();
    if (!id) return;

    this.chargement.set(true);
    this.erreur.set(null);

    // 1. Récupération du personnage (GET /personnages/{id})
    this.personnageService.recuperer(id).subscribe({
      next: (p) => {
        this.personnage.set(p);
      },
      error: (err) =>
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_CHARGEMENT_PERSONNAGE'), err),
    });

    // 2. Récupération de toutes les disciplines disponibles (GET /disciplines)
    this.disciplineService.lister().subscribe({
      next: (disciplines) => {
        this.toutesDisciplines.set(disciplines);
      },
      error: (err) =>
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_CHARGEMENT_DISCIPLINES'), err),
    });

    // 3. Récupération de tous les objets disponibles (GET /objets)
    this.objetService.lister().subscribe({
      next: (objets) => {
        this.tousObjets.set(objets);
      },
      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_CHARGEMENT_OBJETS'), err);
      },
    });

    // 4. Récupération du chapitre (GET /personnages/{id}/chapitre)
    this.chapitreService.getChapitreCourant(id).subscribe({
      next: (data) => {
        this.chapitre.set(data);

        const restants: Record<string, number> = {};
        data.objets.forEach((o) => (restants[o.objetId] = o.valeur));
        this.restantsObjets.set(restants);
        this.lienEnAttenteConfirmation.set(null);

        if (data.id === 2101 || data.id === 2102) {
          const ancien = localStorage.getItem(this.ancienTirageHasardKey);

          this.ancienTirageHasard.set(ancien !== null ? Number(ancien) : null);
        } else {
          this.ancienTirageHasard.set(null);
        }

        if (data.id === 2102) {
          this.chargerEnduranceAvant2102();
        } else {
          this.enduranceAvant2102.set(null);
        }
        this.actualiserCombatDisponible(data.combat, id);

        this.ongletActif.set('chapitre');
        this.ongletLeve.set(null);
        this.ongletObjetsClique.set(this.chapitreObjetDejaClique(data.id));

        const hasardDejaTermine = this.chapitreHasardDejaTermine(data.id);
        this.hasardRoule.set(false);
        this.hasardResultatVisible.set(false);
        this.hasardTermine.set(hasardDejaTermine);
        this.faceHasard.set(hasardDejaTermine ? (data.tirageHasard ?? '?') : '?');

        this.chargement.set(false);
      },
      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_RECUPERATION_CHAPITRE'), err);
        this.erreur.set(this.translate.instant('CHAPITRE_PAGE.ERREUR_CHARGEMENT_CHAPITRE'));
        this.chargement.set(false);
      },
    });
  }

  private actualiserCombatDisponible(estChapitreCombat: boolean, personnageId: string): void {
    if (!estChapitreCombat) {
      this.combatDisponible.set(false);
      this.combatEnDefaite.set(false);
      return;
    }

    // Un combat résolu reste rattaché au chapitre, mais ne doit plus
    // remplacer les liens par le bouton COMBAT au retour sur cette page.
    this.combatService.recuperer(personnageId).subscribe({
      next: (combat) => {
        this.combatDisponible.set(combat.statut === 'EN_COURS');
        this.combatEnDefaite.set(combat.statut === 'DEFAITE');
      },
      error: (err) => {
        // 404 signifie que le combat n'a pas encore été démarré.
        this.combatDisponible.set(err.status === 404);
        this.combatEnDefaite.set(false);
      },
    });
  }

  nomDiscipline(id: string | null): string {
    if (!id) {
      return this.translate.instant('CHAPITRE_PAGE.DISCIPLINE_REQUISE');
    }

    const discipline = this.toutesDisciplines().find(
      (d) => d.id.toLowerCase() === id.toLowerCase(),
    );

    return discipline?.nom ?? id;
  }

  private chapitreObjetDejaClique(chapitreId: number): boolean {
    return this.lireChapitreObjetClique() === chapitreId;
  }

  private enregistrerChapitreObjetClique(): void {
    const chapitreId = this.chapitre()?.id;
    if (chapitreId === undefined) {
      return;
    }

    localStorage.setItem(this.chapitreObjetCliqueKey, String(chapitreId));
  }

  private lireChapitreObjetClique(): number | null {
    const valeur = localStorage.getItem(this.chapitreObjetCliqueKey);
    const chapitreId = valeur === null ? NaN : Number(valeur);
    return Number.isFinite(chapitreId) ? chapitreId : null;
  }

  private chapitreHasardDejaTermine(chapitreId: number): boolean {
    return this.lireChapitreHasardTermine() === this.chapitreHasardTermineValeur(chapitreId);
  }

  private enregistrerChapitreHasardTermine(): void {
    const chapitreId = this.chapitre()?.id;
    if (chapitreId === undefined) {
      return;
    }

    localStorage.setItem(
      this.chapitreHasardTermineKey,
      this.chapitreHasardTermineValeur(chapitreId),
    );
  }

  private lireChapitreHasardTermine(): string | null {
    return localStorage.getItem(this.chapitreHasardTermineKey);
  }

  // Inclut le personnageId : deux personnages passant par le même chapitre
  // (même id numérique) ne doivent pas partager cet état.
  private chapitreHasardTermineValeur(chapitreId: number): string {
    return `${this.personnageId()}:${chapitreId}`;
  }

  nomObjet(id: string | null): string {
    if (!id) {
      return this.translate.instant('CHAPITRE_PAGE.OBJET_REQUIS');
    }

    const objet = this.tousObjets().find((o) => o.id.toLowerCase() === id.toLowerCase());

    return objet?.nom ?? id;
  }

  /**
   * Avance vers un chapitre cible choisi par le joueur.
   *
   * S'il reste au moins un objet à prendre sur ce chapitre, on demande
   * d'abord confirmation (popup) au lieu d'avancer directement : une fois
   * parti, les objets non pris ne sont plus récupérables. `sansConfirmation`
   * sert à la réponse "continuer" du popup et aux redirections automatiques
   * (échec au hasard du 21 / 2101), qui ne sont pas un choix du joueur.
   */
  choisirLien(lien: LienResponse, sansConfirmation = false): void {
    const id = this.personnageId();
    if (!id || !lien.disponible || this.mort() || this.chargement()) return;

    if (!sansConfirmation && this.objetsNonPris().length > 0) {
      this.lienEnAttenteConfirmation.set(lien);
      return;
    }

    this.lienEnAttenteConfirmation.set(null);
    this.chargement.set(true);
    this.chapitreService.avancerVersChapitre(id, lien.chapitreCibleId).subscribe({
      next: () => {
        // Recharge les données mises à jour du personnage et du nouveau chapitre
        this.chargerToutesLesDonnees();
      },
      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_PAGE.ERREUR_AVANCEMENT'), err);
        this.erreur.set(this.translate.instant('CHAPITRE_PAGE.ERREUR_AVANCER_CHAPITRE'));
        this.chargement.set(false);
      },
    });
  }

  /** Popup "objets non pris" : le joueur confirme, on avance quand même. */
  confirmerChangementChapitre(): void {
    const lien = this.lienEnAttenteConfirmation();
    if (!lien) return;

    this.lienEnAttenteConfirmation.set(null);
    this.choisirLien(lien, true);
  }

  /** Popup "objets non pris" : le joueur renonce et va voir les objets. */
  voirObjetsNonPris(): void {
    this.lienEnAttenteConfirmation.set(null);
    // Pas de toggle (contrairement à selectionnerOnglet) : l'onglet doit
    // s'afficher à coup sûr, même s'il était déjà ouvert.
    this.ongletObjetsClique.set(true);
    this.enregistrerChapitreObjetClique();
    this.ongletActif.set('objets');
    this.ongletLeve.set('objets');
    setTimeout(() => this.ongletLeve.set(null), 300);
  }

  /** Popup "objets non pris" : fermé sans rien faire (clic sur le voile). */
  annulerChangementChapitre(): void {
    this.lienEnAttenteConfirmation.set(null);
  }

  /** Redirige vers la page d'accueil des personnages. */
  retourAccueil(): void {
    this.router.navigate(['/accueil']);
  }
}