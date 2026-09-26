import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ActionCombat, CombatEnnemiResponse, CombatResponse, ResultatTourResponse } from '../../../core/models/combat.model';
import { PersonnageResume } from '../../../core/models/personnage.model';
import { CombatService } from '../../../core/services/combat.service';
import { EffetsObjetCombat, InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../core/services/personnage.service';

type Phase = 'TEXTE' | 'MENU' | 'FIN';
type Cible = 'ennemi' | 'joueur' | null;
type BonusSelectionne = 'habilite' | 'arme' | 'puissance' | 'bouclier' | 'garde' | 'resistance-psychique' | 'ennemi-puissance-psychique' | null;

/** Un message de la file d'affichage (boîte de dialogue façon JRPG). */
interface Message {
  txt: string;
  defaite?: boolean;
  ennemiVaincu?: boolean;
  /** Un nouvel ennemi vient de prendre le relais (combat multi-ennemis) :
   * réinitialise le signal ennemiVaincu, sinon l'illustration du nouvel
   * ennemi hériterait de l'état "vaincu" du précédent. */
  nouvelEnnemi?: boolean;
  /** Id de l'ennemi vers lequel avancer ennemiAffocheId quand ce message
   * est joué (voir nouvelEnnemi ci-dessus). */
  ennemiId?: string;
  hit?: Cible;
  actualiser?: 'ennemi' | 'joueur';
  /** ENDURANCE du joueur à afficher dès ce message, avant la mise à jour
   * complète (ex. soin d'une potion affiché avant la riposte). */
  enduranceJoueur?: number;
  /** HABILETÉ temporaire à afficher dès ce message (ex. Essence d'Alether),
   * avant la mise à jour complète. */
  habiliteTempJoueur?: number;
  de?: {
    valeur: number;
  };
  stats?: {
    habilite: number;
    endurance: number;
  };
}

/**
 * Écran de combat en plein écran (route dédiée, hors des tabs de
 * chapitre.page). Design repris d'un prototype fourni (arène + boîte de
 * dialogue avec effet machine à écrire), branché ici sur le vrai backend :
 * POST/GET /personnages/{id}/combat, POST /personnages/{id}/combat/tour.
 * Le serveur fait autorité sur chaque tirage : ce composant ne fait
 * qu'afficher les nombres renvoyés (ResultatTourResponse), il ne calcule
 * jamais lui-même de dégâts.
 */
@Component({
  selector: 'app-combat',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './combat.page.html',
  styleUrl: './combat.page.scss',
})
export class CombatPage implements OnInit, ViewWillEnter, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly combatService = inject(CombatService);
  private readonly personnageService = inject(PersonnageService);
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly translate = inject(TranslateService);

  /** Fond d'arène : foret | brume | crepuscule | pierre | gravure. */
  readonly fond = 'foret';
  private readonly vitesseTexte = 22;

  /**
   * Route de la page des règles du combat (page existante). Ouverte
   * automatiquement au tout premier combat sur cet appareil, puis à la
   * demande via le bouton "?" de l'arène.
   */
  private readonly routeReglesCombat = '/regle/combat';
  private readonly reglesCombatVuesKey = 'loup-solitaire:regles-combat-vues';

  readonly personnageId = signal<string | null>(null);
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly combat = signal<CombatResponse | null>(null);

  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);
  /** Empêche de spammer les boutons pendant qu'un tour est en cours d'envoi. */
  readonly actionEnCours = signal<boolean>(false);
  readonly joueurVaincu = signal(false);
  readonly ennemiVaincu = signal(false);

  // --- Boîte de dialogue (machine à écrire) -------------------------------
  readonly phase = signal<Phase>('TEXTE');
  readonly msg = signal<string>('');
  readonly tape = signal<number>(0);
  readonly tapeEnCours = signal<boolean>(false);
  readonly hit = signal<Cible>(null);
  readonly statsEnnemi = signal<Message['stats'] | null>(null);
  readonly deAttaqueVisible = signal(false);
  readonly deAttaqueRoule = signal(false);
  readonly valeurDeAttaque = signal<number | string>('?');
  readonly texteDe = signal('');
  readonly libelleDe = signal(this.translate.instant('COMBAT_PAGE.JET_ATTAQUE'));
  readonly bonusSelectionne = signal<BonusSelectionne>(null);

  private file: Message[] = [];
  private combatEnAttente: CombatResponse | null = null;
  private personnageEnAttente: PersonnageResume | null = null;
  private joueurVieAppliquee = true;
  private timerTape: ReturnType<typeof setInterval> | null = null;
  private timerHit: ReturnType<typeof setTimeout> | null = null;
  private timerDeFaces: ReturnType<typeof setInterval> | null = null;
  private timerDe: ReturnType<typeof setTimeout> | null = null;

  readonly texte = computed(() => (this.tapeEnCours() ? this.msg().slice(0, this.tape()) : this.msg()));

  /**
   * Id de l'ennemi actuellement affiché (nom, portrait, barre d'ENDURANCE).
   * Distinct du flag `.actif` renvoyé par le backend : dès qu'un ennemi
   * meurt en combat multi-ennemis, le backend bascule immédiatement `.actif`
   * sur le suivant dans la MÊME réponse — si l'UI suivait ce flag
   * directement, portrait/nom/barre changeraient AVANT même que la
   * narration ("X s'effondre, vaincu.") ait fini de s'afficher. On
   * n'avance donc ennemiAffocheId qu'au moment précis où le message
   * "nouvelEnnemi" (voir construireMessages) est joué dans la boîte de
   * dialogue.
   */
  readonly ennemiAffocheId = signal<string | null>(null);

  // --- Données dérivées ---------------------------------------------------
  readonly ennemiActif = computed<CombatEnnemiResponse | null>(() => {
    const c = this.combat();
    if (!c) return null;

    const id = this.ennemiAffocheId();
    return (
      (id ? c.ennemis.find((e) => e.id === id) : undefined) ??
      c.ennemis.find((e) => e.actif) ??
      c.ennemis.find((e) => !e.vaincu) ??
      c.ennemis.find((e) => e.vaincu) ??
      null
    );
  });

  readonly nomJoueur = computed(() => this.personnage()?.nom ?? this.translate.instant('COMBAT_PAGE.NOM_JOUEUR_PAR_DEFAUT'));
  /**
   * Ennemis pas encore vaincus (actif inclus), pour l'effet visuel de
   * "pile de cartes" derrière la plaque de vie ennemie. PAS un computed
   * sur combat() : toutes les cartes disparaissent ensemble à l'instant où
   * l'ennemi actif s'effondre (voir prochain(), m.ennemiVaincu -> 0), et ne
   * réapparaissent qu'au complet quand le suivant est annoncé
   * (m.nouvelEnnemi) — pas de disparition progressive, une à une.
   */
  readonly ennemisRestantsAffiches = signal(0);

  /**
   * Une carte fantôme par ennemi en attente derrière l'actif : pile totale
   * = ennemisRestantsAffiches() (la plaque visible compte pour 1, donc
   * N-1 cartes fantômes ici). Pas de plafond : 3 ennemis restants → pile
   * de 3 (2 cartes fantômes), 4 → pile de 4 (3 cartes fantômes), etc.
   */
  readonly cartesOmbreEnnemis = computed(() => {
    const n = Math.max(0, this.ennemisRestantsAffiches() - 1);
    return Array.from({ length: n }, (_, i) => {
      const rang = i + 1;
      return {
        decalage: rang * 7,
        rotation: -rang * 3,
        opacite: Math.max(0.3, 1 - rang * 0.2),
        // Toujours positif (voir .plaque-ennemi { z-index: 10 } dans le
        // scss) : un z-index négatif place l'élément SOUS tout le décor de
        // fond de l'arène (.decor > div, en z-index auto = 0 implicite),
        // ce qui rendait les cartes invisibles.
        z: 10 - rang,
      };
    });
  });
  /** HAB effective affichée = somme de tous les termes ci-dessous (base +
   * chaque bonus/malus individuel), voir detailHabiliteAffiche() pour le
   * détail terme par terme "13+2+2+2+3" demandé sur la plaque joueur. */
  readonly habiliteJoueur = computed(
    () =>
      this.baseHabiliteJoueur() +
      this.bonusArmeAffiche() +
      this.bonusPsychiqueAffiche() +
      this.bonusTempAffiche() +
      this.bonusGardeAffiche(),
  );

  /**
   * Base "brute" affichée : habiliteBase, PAS habilite (qui inclut déjà
   * l'ajustement d'arme côté backend, voir InventaireService.
   * recalculerHabiliteArmes) — sinon le terme "arme" ci-dessous ferait
   * double emploi avec la base.
   */
  readonly baseHabiliteJoueur = computed(() => this.personnage()?.habiliteBase ?? 0);

  /**
   * Ajustement d'arme isolé : habilite - habiliteBase reproduit exactement
   * la règle backend (malus si aucune arme, +BONUS_ARME_MAITRISEE si arme
   * maîtrisée possédée, 0 sinon) sans avoir à la dupliquer côté front.
   */
  readonly bonusArmeAffiche = computed(() => {
    const p = this.personnage();
    if (!p) return 0;
    return p.habilite - p.habiliteBase;
  });

  readonly bonusPsychiqueAffiche = computed(() => (this.bonusPuissancePsychique() ? 2 : 0));

  /** HABILETÉ temporaire affichée en attendant la fiche à jour (voir
   * Message.habiliteTempJoueur) ; null = valeur réelle du personnage. */
  private readonly habiliteTempForcee = signal<number | null>(null);
  readonly bonusTempAffiche = computed(() => this.habiliteTempForcee() ?? this.personnage()?.habiliteTemp ?? 0);

  readonly bonusGardeAffiche = computed(() => this.combat()?.bonusHabiliteEnAttente ?? 0);

  /**
   * Chaîne "13+2+2+2+3" (base suivie de chaque terme non nul, signe
   * inclus) affichée entre parenthèses à côté du total sur la plaque
   * joueur. Null si aucun bonus/malus actif : on affiche alors juste
   * "HAB 13" sans parenthèses inutiles (voir template).
   */
  readonly detailHabiliteAffiche = computed(() => {
    const termes = [
      this.bonusArmeAffiche(),
      this.bonusPsychiqueAffiche(),
      this.bonusTempAffiche(),
      this.bonusGardeAffiche(),
    ].filter((v) => v !== 0);

    if (termes.length === 0) return null;

    const suffixe = termes.map((v) => (v >= 0 ? '+' + v : String(v))).join('');
    return this.baseHabiliteJoueur() + suffixe;
  });
  /** Valeur d'ENDURANCE affichée en attendant la fiche à jour (voir
   * Message.enduranceJoueur) ; null = valeur réelle du personnage. */
  private readonly enduranceJoueurForcee = signal<number | null>(null);
  readonly enduranceJoueur = computed(
    () => this.enduranceJoueurForcee() ?? this.personnage()?.enduranceActuelle ?? 0,
  );
  readonly enduranceMaxJoueur = computed(() => this.personnage()?.enduranceMax ?? 1);
  readonly bonusHabiliteTemp = computed(() => this.bonusTempAffiche());
  readonly bonusArmeMaitrisee = computed(() => {
    const personnage = this.personnage();
    if (!personnage?.armeMaitrisee) return null;

    // Comparaison par NOM, comme l'inventaire et le chapitre : le backend
    // (PersonnageMapper) envoie armeMaitrisee sous forme de nom ("Masse
    // d'Armes"), pas d'identifiant ("masse"). L'ancienne comparaison par
    // objetId ne marchait que si le nom en minuscules egalait l'identifiant
    // (Lance, Hache...), jamais pour Epee, Masse d'Armes, Marteau de Guerre
    // ni Baton.
    const armePossedee = personnage.inventaire.find(
      (item) =>
        item.categorie === 'ARME' &&
        item.quantite > 0 &&
        item.nom === personnage.armeMaitrisee,
    );

    return armePossedee?.nom ?? null;
  });
  readonly bonusPuissancePsychique = computed(() =>
    (this.personnage()?.disciplines.some((discipline) => discipline.toUpperCase() === 'PUISSANCE_PSYCHIQUE') ?? false) &&
    !(this.ennemiActif()?.resistances ?? []).some(
      (resistance) => resistance.toUpperCase() === 'PUISSANCE_PSYCHIQUE',
    ),
  );

  /** L'ennemi actif est insensible à la Puissance Psychique (résistance côté backend). */
  readonly ennemiResistePuissancePsychique = computed(() =>
    (this.ennemiActif()?.resistances ?? []).some(
      (resistance) => resistance.toUpperCase() === 'PUISSANCE_PSYCHIQUE',
    ),
  );

  /**
   * L'ennemi actif possède la Puissance Psychique (ex. Vordaks) et le joueur
   * n'a pas le Bouclier Psychique pour s'en protéger. Même logique que
   * bonusPuissancePsychique côté joueur, qui disparaît si l'ennemi résiste.
   */
  readonly ennemiPossedePuissancePsychique = computed(() =>
    (this.ennemiActif()?.disciplines ?? []).some(
      (discipline) => discipline.toUpperCase() === 'PUISSANCE_PSYCHIQUE',
    ) && !this.bonusBouclierPsychique(),
  );

  readonly bonusBouclierPsychique = computed(() =>
    (this.personnage()?.disciplines.some((discipline) => discipline.toUpperCase() === 'BOUCLIER_PSYCHIQUE') ?? false)
  );

  readonly bonusGarde = computed(() => (this.combat()?.dernierTour?.reductionPourcent ?? 0) > 0);
  readonly bonusObjets = computed(
    () => this.personnage()?.inventaire.some((item) => item.categorie === 'OBJET' && item.quantite > 0) ?? false,
  );

  ouvrirExplicationBonus(bonus: Exclude<BonusSelectionne, null>): void {
    this.bonusSelectionne.set(bonus);
  }

  fermerExplicationBonus(): void {
    this.bonusSelectionne.set(null);
  }

  readonly statut = computed(() => this.combat()?.statut ?? 'EN_COURS');
  readonly fuitePossible = computed(() => this.combat()?.fuitePossible ?? false);
  /** ASSAUT affiché = tours déjà joués + 1 (celui qui est sur le point de se jouer). */
  readonly round = computed(() => (this.combat()?.assautsLivres ?? 0) + 1);

  readonly pctJoueur = computed(() =>
    this.enduranceMaxJoueur() ? Math.round((this.enduranceJoueur() / this.enduranceMaxJoueur()) * 100) : 0,
  );
  readonly pctEnnemi = computed(() => {
    const e = this.ennemiActif();
    return e && e.enduranceMax ? Math.round((e.enduranceActuelle / e.enduranceMax) * 100) : 0;
  });

  private classeBarre(ratio: number): string {
    if (ratio > 0.5) return 'haut';
    if (ratio > 0.2) return 'moyen';
    return 'bas';
  }
  readonly classeBarreJoueur = computed(() =>
    this.classeBarre(this.enduranceMaxJoueur() ? this.enduranceJoueur() / this.enduranceMaxJoueur() : 1),
  );
  readonly classeBarreEnnemi = computed(() => {
    const e = this.ennemiActif();
    return this.classeBarre(e && e.enduranceMax ? e.enduranceActuelle / e.enduranceMax : 1);
  });

  /**
   * Libellé du bouton final. DEFAITE n'atteint jamais cet écran (voir
   * prochain() / combatCharge()) : ce bouton ne concerne donc plus que
   * VICTOIRE/INTERROMPU, d'où le seul libellé restant.
   */
  readonly libelleFin = computed(() => this.translate.instant('COMBAT_PAGE.BOUTON_POURSUIVRE'));

  ngOnInit(): void {
    this.initialiserId();
  }

  ionViewWillEnter(): void {
    this.initialiserId();
    if (this.personnageId()) {
      this.chargerTout();
    }
  }

  ngOnDestroy(): void {
    if (this.timerTape) clearInterval(this.timerTape);
    if (this.timerHit) clearTimeout(this.timerHit);
    if (this.timerDeFaces) clearInterval(this.timerDeFaces);
    if (this.timerDe) clearTimeout(this.timerDe);
  }

  private initialiserId(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.personnageId.set(id);
    } else {
      this.erreur.set(this.translate.instant('COMBAT_PAGE.ERREUR_IDENTIFIANT_INTROUVABLE'));
      this.chargement.set(false);
    }
  }

  private chargerTout(): void {
    const id = this.personnageId();
    if (!id) return;

    this.chargement.set(true);
    this.erreur.set(null);
    this.joueurVaincu.set(false);
    this.ennemiVaincu.set(false);
    this.ennemiAffocheId.set(null);
    this.ennemisRestantsAffiches.set(0);

    this.personnageService.recuperer(id).subscribe({
      next: (p) => this.personnage.set(p),
      error: (err) => console.error(this.translate.instant('COMBAT_PAGE.ERREUR_CHARGEMENT_PERSONNAGE'), err),
    });

    // Idempotent côté backend : renvoie le combat déjà EN_COURS (ou résolu)
    // s'il existe déjà, sinon 404. On tente donc GET d'abord, et on ne
    // POST (création) qu'en cas de 404 — évite de recréer un combat pour
    // rien à chaque réouverture de l'écran.
    this.combatService.recuperer(id).subscribe({
      next: (c) => this.combatCharge(c),
      error: (err) => {
        if (err.status === 404) {
          this.combatService.initier(id).subscribe({
            next: (c) => this.combatCharge(c),
            error: (e) => this.echecChargement(e),
          });
        } else {
          this.echecChargement(err);
        }
      },
    });
  }

  private echecChargement(err: unknown): void {
    console.error(this.translate.instant('COMBAT_PAGE.ERREUR_CHARGEMENT_COMBAT'), err);
    this.erreur.set(this.translate.instant('COMBAT_PAGE.ERREUR_CHARGEMENT_COMBAT_TEXTE'));
    this.chargement.set(false);
  }

  private combatCharge(c: CombatResponse): void {
    this.combat.set(c);
    this.chargement.set(false);
    // Verrouille l'ennemi affiché sur l'actif du moment (voir le
    // commentaire sur ennemiAffocheId) : il n'avancera plus qu'au rythme
    // de la narration, pas à celui des changements bruts de `.actif`.
    this.ennemiAffocheId.set(
      c.ennemis.find((e) => e.actif)?.id ?? c.ennemis.find((e) => !e.vaincu)?.id ?? null,
    );
    this.ennemisRestantsAffiches.set(c.ennemis.filter((e) => !e.vaincu).length);

    if (c.statut === 'DEFAITE') {
      // Jamais d'écran FIN à bouton pour une défaite (voir prochain()),
      // même en rouvrant un combat déjà résolu : le tap sur ce message
      // renvoie directement au chapitre, où ChapitrePage affiche REVENIR.
      this.jouerFile([{ txt: this.translate.instant('COMBAT_PAGE.MSG_VAINCU'), defaite: true }]);
      return;
    }

    if (c.statut !== 'EN_COURS') {
      // Combat déjà résolu (écran rouvert après coup) : pas de rejouer la
      // scène, direction l'écran de fin.
      this.phase.set('FIN');
      this.msg.set(this.messageResolu(c.statut));
      return;
    }

    if (c.assautsLivres === 0) {
      // Tout premier combat sur cet appareil : on affiche d'abord la page
      // des règles. Le drapeau est posé AVANT de partir : au retour,
      // ionViewWillEnter -> chargerTout -> combatCharge repasse ici et
      // joue alors l'intro normalement, sans renvoyer en boucle vers les
      // règles. Si le stockage est indisponible, on ne redirige pas (sinon
      // boucle infinie) : le combat démarre directement.
      if (!this.reglesDejaVues() && this.marquerReglesVues()) {
        this.ouvrirRegles();
        return;
      }
      this.jouerIntro();
    } else {
      // Combat repris en cours (ex. rechargement de page) : direct au menu.
      this.phase.set('MENU');
      this.msg.set('');
    }
  }

  /** Scène d'introduction d'un combat tout juste initié. */
  private jouerIntro(): void {
    const ennemi = this.ennemiActif();
    if (!ennemi) {
      this.phase.set('MENU');
      return;
    }

    this.jouerFile([
      { txt: this.translate.instant('COMBAT_PAGE.MSG_BARRE_ROUTE', { nom: ennemi.nom }) },
      {
        txt: this.translate.instant('COMBAT_PAGE.MSG_STATS_ENNEMI', { habilite: ennemi.habilite, endurance: ennemi.enduranceMax }),
        stats: { habilite: ennemi.habilite, endurance: ennemi.enduranceMax },
      },
    ]);
  }

  /* ---------------------------------------------------------------- règles */

  private reglesDejaVues(): boolean {
    try {
      return localStorage.getItem(this.reglesCombatVuesKey) === '1';
    } catch {
      return false;
    }
  }

  /** Renvoie false si le stockage est indisponible (navigation privée...). */
  private marquerReglesVues(): boolean {
    try {
      localStorage.setItem(this.reglesCombatVuesKey, '1');
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Ouvre la page des règles en lui passant l'id du personnage : elle
   * affiche alors ses boutons "retour au combat" et ramène à
   * /personnages/{id}/combat (voir RegleCombatPage). On ne passe PAS
   * this.router.url : appelé depuis combatCharge() pendant l'entrée sur
   * cet écran, il peut encore valoir l'URL de la page précédente.
   */
  ouvrirRegles(): void {
    const id = this.personnageId();
    if (!id || this.actionEnCours()) return;
    this.router.navigate([this.routeReglesCombat], {
      queryParams: { combat: id },
    });
  }

  private messageResolu(statut: string): string {
    switch (statut) {
      case 'VICTOIRE':
        return this.translate.instant('COMBAT_PAGE.MSG_DEJA_TRIOMPHE');
      case 'FUITE':
        return this.translate.instant('COMBAT_PAGE.MSG_DEJA_FUI');
      default:
        return this.translate.instant('COMBAT_PAGE.MSG_COMBAT_TERMINE');
    }
  }

  /* --------------------------------------------------------------- actions */

  agir(action: ActionCombat): void {
    if (this.phase() !== 'MENU' || this.statut() !== 'EN_COURS' || this.actionEnCours()) return;

    if (action === 'FUITE' && !this.fuitePossible()) {
      this.jouerFile([{ txt: this.translate.instant('COMBAT_PAGE.MSG_IMPOSSIBLE_FUIR') }]);
      return;
    }

    this.appellerTour(action);
  }

  ouvrirSac(): void {
    const id = this.personnageId();
    if (!id) return;
    // En combat en cours, utiliser un objet remplace l'attaque ou la
    // défense (action OBJET, l'ennemi riposte) : REGLE-05.
    if (this.statut() === 'EN_COURS') {
      this.inventaireSheet.ouvrir(id, (objetId, nom, effets) => this.utiliserObjet(objetId, nom, effets));
    } else {
      this.inventaireSheet.ouvrir(id);
    }
  }

  private utiliserObjet(objetId: string, nom: string, effets: EffetsObjetCombat): void {
    if (this.phase() !== 'MENU' || this.statut() !== 'EN_COURS' || this.actionEnCours()) return;
    this.objetUtiliseNom = nom;
    this.objetUtiliseSoin = effets.endurance;
    this.objetUtiliseBonusHabilite = effets.habilite;
    this.appellerTour('OBJET', objetId);
  }

  /** Nom de l'objet joué au dernier tour OBJET, pour le message du combat. */
  private objetUtiliseNom: string | null = null;
  private objetUtiliseSoin = 0;
  private objetUtiliseBonusHabilite = 0;

  private appellerTour(action: ActionCombat, objetId?: string): void {
    const id = this.personnageId();
    if (!id || this.actionEnCours()) return;

    this.actionEnCours.set(true);
    const ennemiAvant = this.ennemiActif();
    const enduranceJoueurAvant = this.enduranceJoueur();
    const habiliteTempAvant = this.bonusTempAffiche();

    if (action === 'ATTAQUE' || action === 'DEFENSE') {
      this.phase.set('TEXTE');
      this.msg.set('');
      this.deAttaqueVisible.set(true);
      this.deAttaqueRoule.set(true);
      this.texteDe.set(
        action === 'ATTAQUE'
          ? this.translate.instant('COMBAT_PAGE.MSG_PORTE_ATTAQUE', { nom: this.nomJoueur() })
          : this.translate.instant('COMBAT_PAGE.MSG_LEVE_GARDE', { nom: this.nomJoueur() }),
      );
      this.libelleDe.set(
        action === 'ATTAQUE'
          ? this.translate.instant('COMBAT_PAGE.JET_ATTAQUE')
          : this.translate.instant('COMBAT_PAGE.JET_DEFENSE'),
      );
      this.valeurDeAttaque.set('?');
      this.timerDeFaces = setInterval(() => {
        this.valeurDeAttaque.set(Math.floor(Math.random() * 10));
      }, 90);
    }

    this.combatService.jouerTour(id, action, objetId).subscribe({
      next: (c) => {
        const messages = this.construireMessages(action, c, ennemiAvant, enduranceJoueurAvant, habiliteTempAvant);

        const afficherResultat = () => {
          this.actionEnCours.set(false);
          this.combatEnAttente = c;
          this.joueurVieAppliquee = false;
          this.personnageService.recuperer(id).subscribe({
            next: (p) => {
              this.personnageEnAttente = p;
              if (this.joueurVieAppliquee) {
                this.personnage.set(p);
              }
            },
            error: (err) => console.error(this.translate.instant('COMBAT_PAGE.ERREUR_RECHARGEMENT_PERSONNAGE'), err),
          });
          this.deAttaqueVisible.set(false);
          this.jouerFile(action === 'ATTAQUE' ? messages.slice(1) : messages);
        };

        if (action === 'ATTAQUE' || action === 'DEFENSE') {
          const tirage = action === 'ATTAQUE' ? c.dernierTour?.tirageAttaque : c.dernierTour?.tirageDefense;
          this.file = messages.slice(1);
          this.combatEnAttente = c;
          this.joueurVieAppliquee = false;
          this.personnageService.recuperer(id).subscribe({
            next: (p) => {
              this.personnageEnAttente = p;
              if (this.joueurVieAppliquee) {
                this.personnage.set(p);
              }
            },
            error: (err) => console.error(this.translate.instant('COMBAT_PAGE.ERREUR_RECHARGEMENT_PERSONNAGE'), err),
          });
          this.timerDe = setTimeout(() => {
            if (this.timerDeFaces) clearInterval(this.timerDeFaces);
            this.valeurDeAttaque.set(tirage ?? '?');
            this.deAttaqueRoule.set(false);
            this.actionEnCours.set(false);
          }, 850);
        } else {
          // FUITE comprise (REGLE-03) : la riposte met à jour la barre
          // d'ENDURANCE du joueur au bon moment, comme pour OBJET.
          afficherResultat();
        }
      },
      error: (err) => {
        this.actionEnCours.set(false);
        if (this.timerDeFaces) clearInterval(this.timerDeFaces);
        this.deAttaqueVisible.set(false);
        this.deAttaqueRoule.set(false);
        console.error(this.translate.instant('COMBAT_PAGE.ERREUR_TOUR_COMBAT'), err);
        this.jouerFile([{ txt: this.translate.instant('COMBAT_PAGE.MSG_ERREUR_REESSAYER') }]);
      },
    });
  }

  /** Traduit un ResultatTourResponse (nombres bruts du serveur) en phrases. */
  private construireMessages(
    action: ActionCombat,
    combat: CombatResponse,
    ennemiAvant: CombatEnnemiResponse | null,
    enduranceJoueurAvant: number,
    habiliteTempAvant: number,
  ): Message[] {
    const tour = combat.dernierTour;
    const nomEnnemi = ennemiAvant?.nom ?? "l'ennemi";
    const messages: Message[] = [];

    if (!tour) {
      return messages;
    }

    if (action === 'FUITE') {
      // REGLE-03 : l'ennemi porte un dernier coup pendant la fuite (bloc
      // "riposte" ci-dessous) ; le message de fuite ne vient qu'ensuite, et
      // seulement si ce coup n'a pas été fatal.
      messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_TENTE_FUITE', { nom: this.nomJoueur() }) });
    } else if (action === 'OBJET') {
      messages.push({
        txt: this.translate.instant('COMBAT_PAGE.MSG_UTILISE_OBJET', {
          nom: this.nomJoueur(),
          objet: this.objetUtiliseNom ?? '',
        }),
        // Le soin apparaît sur la barre dès ce message ; la riposte qui suit
        // applique ensuite la valeur finale renvoyée par le serveur.
        enduranceJoueur:
          this.objetUtiliseSoin > 0
            ? Math.min(this.enduranceMaxJoueur(), enduranceJoueurAvant + this.objetUtiliseSoin)
            : undefined,
        // Même principe pour l'HABILETÉ temporaire (total et détail).
        habiliteTempJoueur:
          this.objetUtiliseBonusHabilite !== 0
            ? habiliteTempAvant + this.objetUtiliseBonusHabilite
            : undefined,
      });
    } else if (action === 'ATTAQUE') {
      messages.push({
        txt: this.translate.instant('COMBAT_PAGE.MSG_PORTE_ATTAQUE', { nom: this.nomJoueur() }),
        de: { valeur: tour.tirageAttaque ?? 0 },
      });
    } else if (action === 'DEFENSE') {
      messages.push({
        txt: this.translate.instant('COMBAT_PAGE.MSG_LEVE_GARDE', { nom: this.nomJoueur() }),
        de: { valeur: tour.tirageDefense ?? 0 },
      });
    }

    if (tour.degatsInfliges !== null) {
      const degats = -tour.degatsInfliges;
      // -999 = coup fatal net dans TABLE_DEGATS_INFLIGES côté backend (voir
      // TableCombatService), pas un vrai total de 999 points d'ENDURANCE.
      const coupFatal = tour.degatsInfliges <= -999;
      messages.push({
        txt: coupFatal
          ? this.translate.instant('COMBAT_PAGE.MSG_COUP_FATAL_ENNEMI', { nom: nomEnnemi })
          : degats > 0
            ? this.translate.instant('COMBAT_PAGE.MSG_PERD_ENDURANCE_ENNEMI', { nom: nomEnnemi, degats })
            : this.translate.instant('COMBAT_PAGE.MSG_PARE_COUP', { nom: nomEnnemi }),
        hit: degats > 0 ? 'ennemi' : null,
        actualiser: 'ennemi',
      });
    }

    const ennemiVaincu = ennemiAvant && combat.ennemis.find((e) => e.id === ennemiAvant.id)?.vaincu;
    if (action === 'ATTAQUE' && ennemiVaincu) {
      messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_EFFONDRE_VAINCU', { nom: nomEnnemi }), ennemiVaincu: true });

      // Combat multi-ennemis (voir CombatService.passerAuProchainEnnemi côté
      // backend) : un nouvel adversaire prend le relais dans le même
      // combat. Sans cette introduction, son illustration hériterait
      // silencieusement de la classe "vaincu" du précédent, et sa barre
      // d'ENDURANCE apparaîtrait sans transition ni explication.
      const nouvelEnnemiActif = combat.ennemis.find((e) => e.actif && e.id !== ennemiAvant?.id);
      if (nouvelEnnemiActif) {
        messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_PREND_RELAIS', { nom: nouvelEnnemiActif.nom }) });
        messages.push({
          txt: this.translate.instant('COMBAT_PAGE.MSG_STATS_ENNEMI', {
            habilite: nouvelEnnemiActif.habilite,
            endurance: nouvelEnnemiActif.enduranceMax,
          }),
          stats: { habilite: nouvelEnnemiActif.habilite, endurance: nouvelEnnemiActif.enduranceMax },
          nouvelEnnemi: true,
          ennemiId: nouvelEnnemiActif.id,
        });
      }
    }

    if (tour.degatsSubis !== null) {
      const degats = -tour.degatsSubis;
      // -999 = coup fatal net dans TABLE_DEGATS_SUBIS côté backend. On se
      // base sur degatsSubisBruts (avant réduction de garde), pas sur
      // degatsSubis : une DEFENSE peut réduire -999 à un nombre "normal"
      // (ex. -749 à 75% de dégâts bruts) qui, sans ça, afficherait
      // simplement "Vous perdez 749 points d'ENDURANCE." au lieu du
      // message spécial.
      const coupFatal = (tour.degatsSubisBruts ?? tour.degatsSubis) <= -999;
      messages.push({
        txt: this.translate.instant('COMBAT_PAGE.MSG_RIPOSTE', { nom: nomEnnemi }),
        de: { valeur: tour.tirageRiposte ?? 0 },
      });
      let txt: string;
      if (coupFatal && degats > 0) {
        txt = this.translate.instant('COMBAT_PAGE.MSG_COUP_FATAL_JOUEUR');
      } else if (degats <= 0) {
        txt = this.translate.instant('COMBAT_PAGE.MSG_ESQUIVE');
      } else if (action === 'DEFENSE' && tour.reductionPourcent) {
        txt = this.translate.instant('COMBAT_PAGE.MSG_PERD_ENDURANCE_GARDE', {
          degats,
          pourcent: tour.reductionPourcent,
        });
      } else {
        txt = this.translate.instant('COMBAT_PAGE.MSG_PERD_ENDURANCE', { degats });
      }
      messages.push({ txt, hit: degats > 0 ? 'joueur' : null, actualiser: 'joueur' });
    }

    // "Vous saisissez une ouverture" n'a de sens que si le combat continue
    // ensuite : un bonus d'HABILITE "pour votre prochaine attaque" alors
    // qu'on vient d'être vaincu sur ce même tour n'a aucun sens narratif.
    if (action === 'DEFENSE' && tour.bonusHabiliteObtenu && combat.statut === 'EN_COURS') {
      messages.push({
        txt: this.translate.instant('COMBAT_PAGE.MSG_OUVERTURE', { bonus: tour.bonusHabiliteObtenu }),
      });
    }

    if (combat.statut === 'FUITE') {
      messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_FUITE') });
    } else if (combat.statut === 'DEFAITE') {
      messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_VAINCU'), defaite: true });
    } else if (combat.statut === 'INTERROMPU') {
      messages.push({ txt: this.translate.instant('COMBAT_PAGE.MSG_INTERROMPU') });
    }

    return messages;
  }

  /** Touche sur la boîte de dialogue : termine la frappe, puis passe au message suivant. */
  avancer(event?: Event): void {
    if (event && (event.target as HTMLElement)?.closest('button')) return;
    if (this.phase() === 'FIN') {
      this.terminer();
      return;
    }
    if (this.phase() !== 'TEXTE') return;

    if (this.deAttaqueVisible()) {
      if (this.deAttaqueRoule()) return;
      this.deAttaqueVisible.set(false);
      this.prochain();
      return;
    }

    if (this.tapeEnCours()) {
      if (this.timerTape) clearInterval(this.timerTape);
      this.tape.set(this.msg().length);
      this.tapeEnCours.set(false);
      return;
    }
    this.prochain();
  }

  /**
   * Point de sortie unique de l'écran de combat, quel que soit le statut
   * (VICTOIRE via l'écran FIN, FUITE ou DEFAITE via prochain() qui saute
   * cet écran). Pour DEFAITE, revenirApresDefaite() n'est PLUS appelé
   * ici : ChapitrePage détecte combatEnDefaite() et affiche son propre
   * bouton REVENIR (1 PIÈCE PREMIUM), qui appelle cette méthode — évite un
   * appel en double si le joueur revient sur cet écran de combat résolu.
   */
  terminer(): void {
    const id = this.personnageId();
    if (!id) return;
    this.router.navigate(['/personnages', id, 'chapitre']);
  }

  /* --------------------------------------------------------- file de messages */

  private jouerFile(file: Message[]): void {
    this.file = file.slice();
    this.phase.set('TEXTE');
    this.msg.set('');
    this.prochain();
  }

  private appliquerMiseAJourEnAttente(cible?: Message['actualiser']): void {
    if (this.combatEnAttente && cible) {
      this.combat.set(this.combatEnAttente);
      this.combatEnAttente = null;
    }
    if (cible === 'joueur') {
      this.enduranceJoueurForcee.set(null);
      this.habiliteTempForcee.set(null);
      this.joueurVieAppliquee = true;
      if (this.personnageEnAttente) {
        this.personnage.set(this.personnageEnAttente);
        this.personnageEnAttente = null;
      }
    }
  }

  private prochain(): void {
    if (!this.file.length) {
      if (this.statut() !== 'EN_COURS') {
        // Ni l'un ni l'autre ne passe par l'écran FIN à bouton : le dernier
        // message de la boîte de dialogue ("Vous rompez le combat...' /
        // "Vous avez été vaincu.") suffit, un tap dessus renvoie
        // directement au chapitre. Pour DEFAITE, c'est ChapitrePage qui
        // affiche ensuite REVENIR (1 PIÈCE PREMIUM) et appelle
        // revenirApresDefaite() — pas cet écran.
        this.terminer();
        return;
      }
      this.phase.set('MENU');
      this.msg.set('');
      return;
    }

    const m = this.file.shift()!;
    this.appliquerMiseAJourEnAttente(m.actualiser);
    if (m.enduranceJoueur !== undefined) {
      this.enduranceJoueurForcee.set(m.enduranceJoueur);
    }
    if (m.habiliteTempJoueur !== undefined) {
      this.habiliteTempForcee.set(m.habiliteTempJoueur);
    }
    if (m.ennemiVaincu) {
      this.ennemiVaincu.set(true);
      // Toutes les cartes disparaissent ensemble à la mort de l'ennemi
      // actif, plutôt que de se réduire une à une : elles ne réapparaîtront
      // qu'au complet quand le message "nouvelEnnemi" annoncera le suivant.
      this.ennemisRestantsAffiches.set(0);
    }
    if (m.nouvelEnnemi) {
      this.ennemiVaincu.set(false);
      if (m.ennemiId) this.ennemiAffocheId.set(m.ennemiId);
      this.ennemisRestantsAffiches.set(this.combat()?.ennemis.filter((e) => !e.vaincu).length ?? 0);
    }
    if (m.defaite) {
      this.joueurVaincu.set(true);
    }

    if (m.de) {
      if (this.timerDeFaces) clearInterval(this.timerDeFaces);
      if (this.timerDe) clearTimeout(this.timerDe);
      this.phase.set('TEXTE');
      this.texteDe.set(m.txt);
      this.libelleDe.set(this.translate.instant('COMBAT_PAGE.JET_RIPOSTE'));
      this.deAttaqueVisible.set(true);
      this.deAttaqueRoule.set(true);
      this.valeurDeAttaque.set('?');
      this.timerDeFaces = setInterval(() => {
        this.valeurDeAttaque.set(Math.floor(Math.random() * 10));
      }, 90);
      this.timerDe = setTimeout(() => {
        if (this.timerDeFaces) clearInterval(this.timerDeFaces);
        this.valeurDeAttaque.set(m.de!.valeur);
        this.deAttaqueRoule.set(false);
      }, 850);
      return;
    }

    if (m.hit) {
      this.hit.set(m.hit);
      if (this.timerHit) clearTimeout(this.timerHit);
      this.timerHit = setTimeout(() => this.hit.set(null), 520);
    }

    this.phase.set('TEXTE');
    this.msg.set(m.txt);
    this.statsEnnemi.set(m.stats ?? null);
    this.taper(m.txt);
  }

  private taper(txt: string): void {
    if (this.timerTape) clearInterval(this.timerTape);
    this.tape.set(0);
    this.tapeEnCours.set(true);

    let i = 0;
    this.timerTape = setInterval(() => {
      i += 1;
      this.tape.set(i);
      if (i >= txt.length) {
        if (this.timerTape) clearInterval(this.timerTape);
        this.tapeEnCours.set(false);
      }
    }, this.vitesseTexte);
  }
}