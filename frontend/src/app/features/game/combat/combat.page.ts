import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';

import { ActionCombat, CombatEnnemiResponse, CombatResponse, ResultatTourResponse } from '../../../core/models/combat.model';
import { PersonnageResume } from '../../../core/models/personnage.model';
import { CombatService } from '../../../core/services/combat.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../core/services/personnage.service';

type Phase = 'TEXTE' | 'MENU' | 'FIN';
type Cible = 'ennemi' | 'joueur' | null;
type BonusSelectionne = 'habilite' | 'arme' | 'puissance' | 'bouclier' | 'garde' | null;

/** Un message de la file d'affichage (boîte de dialogue façon JRPG). */
interface Message {
  txt: string;
  hit?: Cible;
  actualiser?: 'ennemi' | 'joueur';
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
  imports: [IonContent],
  templateUrl: './combat.page.html',
  styleUrl: './combat.page.scss',
})
export class CombatPage implements OnInit, ViewWillEnter, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly combatService = inject(CombatService);
  private readonly personnageService = inject(PersonnageService);
  private readonly inventaireSheet = inject(InventaireSheetService);

  /** Fond d'arène : foret | brume | crepuscule | pierre | gravure. */
  readonly fond = 'foret';
  private readonly vitesseTexte = 22;

  readonly personnageId = signal<string | null>(null);
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly combat = signal<CombatResponse | null>(null);

  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);
  /** Empêche de spammer les boutons pendant qu'un tour est en cours d'envoi. */
  readonly actionEnCours = signal<boolean>(false);

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
  readonly libelleDe = signal("JET D'ATTAQUE");
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

  // --- Données dérivées ---------------------------------------------------
  readonly ennemiActif = computed<CombatEnnemiResponse | null>(() => {
    const c = this.combat();
    return c?.ennemis.find((e) => e.actif) ?? c?.ennemis.find((e) => !e.vaincu) ?? null;
  });

  readonly nomJoueur = computed(() => this.personnage()?.nom ?? 'Loup Solitaire');
  readonly habiliteJoueur = computed(
    () => (this.personnage()?.habilite ?? 0) + (this.personnage()?.habiliteTemp ?? 0),
  );
  readonly enduranceJoueur = computed(() => this.personnage()?.enduranceActuelle ?? 0);
  readonly enduranceMaxJoueur = computed(() => this.personnage()?.enduranceMax ?? 1);
  readonly bonusHabiliteTemp = computed(() => this.personnage()?.habiliteTemp ?? 0);
  readonly bonusArmeMaitrisee = computed(() => {
    const personnage = this.personnage();
    if (!personnage?.armeMaitrisee) return null;

    const armePossedee = personnage.inventaire.find(
      (item) =>
        item.categorie === 'ARME' &&
        item.quantite > 0 &&
        item.objetId.toLowerCase() === personnage.armeMaitrisee?.toLowerCase(),
    );

    return armePossedee?.nom ?? null;
  });
  readonly bonusPuissancePsychique = computed(() =>
    (this.personnage()?.disciplines.some((discipline) => discipline.toUpperCase() === 'PUISSANCE_PSYCHIQUE') ?? false) &&
    !(this.ennemiActif()?.resistances ?? []).some(
      (resistance) => resistance.toUpperCase() === 'PUISSANCE_PSYCHIQUE',
    ),
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
  readonly libelleFin = computed(() => 'POURSUIVRE');

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
      this.erreur.set('Identifiant de personnage introuvable.');
      this.chargement.set(false);
    }
  }

  private chargerTout(): void {
    const id = this.personnageId();
    if (!id) return;

    this.chargement.set(true);
    this.erreur.set(null);

    this.personnageService.recuperer(id).subscribe({
      next: (p) => this.personnage.set(p),
      error: (err) => console.error('Erreur lors du chargement du personnage :', err),
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
    console.error('Erreur lors du chargement du combat :', err);
    this.erreur.set('Impossible de charger le combat.');
    this.chargement.set(false);
  }

  private combatCharge(c: CombatResponse): void {
    this.combat.set(c);
    this.chargement.set(false);

    if (c.statut === 'DEFAITE') {
      // Jamais d'écran FIN à bouton pour une défaite (voir prochain()),
      // même en rouvrant un combat déjà résolu : le tap sur ce message
      // renvoie directement au chapitre, où ChapitrePage affiche REVENIR.
      this.jouerFile([{ txt: 'Vous avez été vaincu.' }]);
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
      // Combat tout juste initié : scène d'introduction.
      const ennemi = this.ennemiActif();
      if (ennemi) {
        this.jouerFile([
          { txt: `${ennemi.nom} vous barre la route !` },
          {
            txt: `HABILETÉ ${ennemi.habilite} — ENDURANCE ${ennemi.enduranceMax}.`,
            stats: { habilite: ennemi.habilite, endurance: ennemi.enduranceMax },
          },
        ]);
      } else {
        this.phase.set('MENU');
      }
    } else {
      // Combat repris en cours (ex. rechargement de page) : direct au menu.
      this.phase.set('MENU');
      this.msg.set('');
    }
  }

  private messageResolu(statut: string): string {
    switch (statut) {
      case 'VICTOIRE':
        return 'Vous avez déjà triomphé de cet adversaire.';
      case 'FUITE':
        return 'Vous avez déjà fui ce combat.';
      default:
        return 'Ce combat est terminé.';
    }
  }

  /* --------------------------------------------------------------- actions */

  agir(action: ActionCombat): void {
    if (this.phase() !== 'MENU' || this.statut() !== 'EN_COURS' || this.actionEnCours()) return;

    if (action === 'FUITE' && !this.fuitePossible()) {
      this.jouerFile([{ txt: 'Impossible de rompre ce combat.' }]);
      return;
    }

    this.appellerTour(action);
  }

  ouvrirSac(): void {
    const id = this.personnageId();
    if (id) {
      this.inventaireSheet.ouvrir(id);
    }
  }

  private appellerTour(action: ActionCombat, objetId?: string): void {
    const id = this.personnageId();
    if (!id || this.actionEnCours()) return;

    this.actionEnCours.set(true);
    const ennemiAvant = this.ennemiActif();
    const enduranceJoueurAvant = this.enduranceJoueur();

    if (action === 'ATTAQUE' || action === 'DEFENSE') {
      this.phase.set('TEXTE');
      this.msg.set('');
      this.deAttaqueVisible.set(true);
      this.deAttaqueRoule.set(true);
      this.texteDe.set(
        action === 'ATTAQUE' ? `${this.nomJoueur()} porte son attaque !` : `${this.nomJoueur()} lève sa garde.`,
      );
      this.libelleDe.set(action === 'ATTAQUE' ? "JET D'ATTAQUE" : 'JET DE DÉFENSE');
      this.valeurDeAttaque.set('?');
      this.timerDeFaces = setInterval(() => {
        this.valeurDeAttaque.set(Math.floor(Math.random() * 10));
      }, 90);
    }

    this.combatService.jouerTour(id, action, objetId).subscribe({
      next: (c) => {
        const messages = this.construireMessages(action, c, ennemiAvant, enduranceJoueurAvant);

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
            error: (err) => console.error('Erreur lors du rechargement du personnage :', err),
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
            error: (err) => console.error('Erreur lors du rechargement du personnage :', err),
          });
          this.timerDe = setTimeout(() => {
            if (this.timerDeFaces) clearInterval(this.timerDeFaces);
            this.valeurDeAttaque.set(tirage ?? '?');
            this.deAttaqueRoule.set(false);
            this.actionEnCours.set(false);
          }, 850);
        } else {
          if (action === 'FUITE') {
            this.combat.set(c);
            this.combatEnAttente = null;
            this.jouerFile(messages);
          } else {
            afficherResultat();
          }
        }
      },
      error: (err) => {
        this.actionEnCours.set(false);
        if (this.timerDeFaces) clearInterval(this.timerDeFaces);
        this.deAttaqueVisible.set(false);
        this.deAttaqueRoule.set(false);
        console.error('Erreur lors du tour de combat :', err);
        this.jouerFile([{ txt: "Une erreur est survenue, réessayez." }]);
      },
    });
  }

  /** Traduit un ResultatTourResponse (nombres bruts du serveur) en phrases. */
  private construireMessages(
    action: ActionCombat,
    combat: CombatResponse,
    ennemiAvant: CombatEnnemiResponse | null,
    enduranceJoueurAvant: number,
  ): Message[] {
    const tour = combat.dernierTour;
    const nomEnnemi = ennemiAvant?.nom ?? "l'ennemi";
    const messages: Message[] = [];

    if (!tour) {
      return messages;
    }

    if (action === 'FUITE') {
      messages.push({ txt: 'Vous rompez le combat et disparaissez !!' });
      return messages;
    }

    if (action === 'ATTAQUE') {
      messages.push({
        txt: `${this.nomJoueur()} porte son attaque !`,
        de: { valeur: tour.tirageAttaque ?? 0 },
      });
    } else if (action === 'DEFENSE') {
      messages.push({
        txt: `${this.nomJoueur()} lève sa garde.`,
        de: { valeur: tour.tirageDefense ?? 0 },
      });
    }

    if (tour.degatsInfliges !== null) {
      const degats = -tour.degatsInfliges;
      messages.push({
        txt: degats > 0 ? `${nomEnnemi} perd ${degats} points d'ENDURANCE.` : `${nomEnnemi} pare le coup. Aucun dégât.`,
        hit: degats > 0 ? 'ennemi' : null,
        actualiser: 'ennemi',
      });
    }

    const ennemiVaincu = ennemiAvant && combat.ennemis.find((e) => e.id === ennemiAvant.id)?.vaincu;
    if (action === 'ATTAQUE' && ennemiVaincu) {
      messages.push({ txt: `${nomEnnemi} s'effondre, vaincu.` });
    }

    if (tour.degatsSubis !== null) {
      const degats = -tour.degatsSubis;
      messages.push({
        txt: `${nomEnnemi} riposte !`,
        de: { valeur: tour.tirageRiposte ?? 0 },
      });
      let txt: string;
      if (degats <= 0) {
        txt = 'Vous esquivez le coup.';
      } else if (action === 'DEFENSE' && tour.reductionPourcent) {
        txt = `Vous perdez ${degats} points d'ENDURANCE \n( Garde amortie de ${tour.reductionPourcent}% ).`;
      } else {
        txt = `Vous perdez ${degats} points d'ENDURANCE.`;
      }
      messages.push({ txt, hit: degats > 0 ? 'joueur' : null, actualiser: 'joueur' });
    }

    // "Vous saisissez une ouverture" n'a de sens que si le combat continue
    // ensuite : un bonus d'HABILETÉ "pour votre prochaine attaque" alors
    // qu'on vient d'être vaincu sur ce même tour n'a aucun sens narratif.
    if (action === 'DEFENSE' && tour.bonusHabiliteObtenu && combat.statut === 'EN_COURS') {
      messages.push({
        txt: `Vous saisissez une ouverture :\n +${tour.bonusHabiliteObtenu} HABILITÉ pour votre prochaine attaque.`,
      });
    }

    if (combat.statut === 'DEFAITE') {
      messages.push({ txt: 'Vous avez été vaincu.' });
    } else if (combat.statut === 'INTERROMPU') {
      messages.push({ txt: 'Le combat est interrompu ; le récit continue.' });
    }

    return messages;
  }

  /** Touche sur la boîte de dialogue : termine la frappe, puis passe au message suivant. */
  avancer(event?: Event): void {
    if (event && (event.target as HTMLElement)?.closest('button')) return;
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
      this.joueurVieAppliquee = true;
      if (this.personnageEnAttente) {
        this.personnage.set(this.personnageEnAttente);
        this.personnageEnAttente = null;
      }
    }
  }

  private prochain(): void {
    if (!this.file.length) {
      if (this.statut() === 'FUITE' || this.statut() === 'DEFAITE') {
        // Ni l'un ni l'autre ne passe par l'écran FIN à bouton : le dernier
        // message de la boîte de dialogue ("Vous rompez le combat...' /
        // "Vous avez été vaincu.") suffit, un tap dessus renvoie
        // directement au chapitre. Pour DEFAITE, c'est ChapitrePage qui
        // affiche ensuite REVENIR (1 PIÈCE PREMIUM) et appelle
        // revenirApresDefaite() — pas cet écran.
        this.terminer();
        return;
      }
      const fin = this.statut() !== 'EN_COURS';
      this.phase.set(fin ? 'FIN' : 'MENU');
      if (!fin) this.msg.set('');
      return;
    }

    const m = this.file.shift()!;
    this.appliquerMiseAJourEnAttente(m.actualiser);

    if (m.de) {
      if (this.timerDeFaces) clearInterval(this.timerDeFaces);
      if (this.timerDe) clearTimeout(this.timerDe);
      this.phase.set('TEXTE');
      this.texteDe.set(m.txt);
      this.libelleDe.set('JET DE RIPOSTE');
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