import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';

import { ActionCombat, CombatEnnemiResponse, CombatResponse, ResultatTourResponse } from '../../../core/models/combat.model';
import { InventaireItem, ObjetResume, PersonnageResume } from '../../../core/models/personnage.model';
import { CombatService } from '../../../core/services/combat.service';
import { ObjetService } from '../../../core/services/objet.service';
import { PersonnageService } from '../../../core/services/personnage.service';

type Phase = 'TEXTE' | 'MENU' | 'FIN';
type Cible = 'ennemi' | 'joueur' | null;

/** Un message de la file d'affichage (boîte de dialogue façon JRPG). */
interface Message {
  txt: string;
  hit?: Cible;
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
  private readonly objetService = inject(ObjetService);

  /** Fond d'arène : foret | brume | crepuscule | pierre | gravure. */
  readonly fond = 'foret';
  private readonly vitesseTexte = 22;

  readonly personnageId = signal<string | null>(null);
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly tousObjets = signal<ObjetResume[]>([]);
  readonly combat = signal<CombatResponse | null>(null);

  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);
  /** Empêche de spammer les boutons pendant qu'un tour est en cours d'envoi. */
  readonly actionEnCours = signal<boolean>(false);
  readonly sacOuvert = signal<boolean>(false);

  // --- Boîte de dialogue (machine à écrire) -------------------------------
  readonly phase = signal<Phase>('TEXTE');
  readonly msg = signal<string>('');
  readonly tape = signal<number>(0);
  readonly tapeEnCours = signal<boolean>(false);
  readonly hit = signal<Cible>(null);

  private file: Message[] = [];
  private timerTape: ReturnType<typeof setInterval> | null = null;
  private timerHit: ReturnType<typeof setTimeout> | null = null;

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

  /** Libellé du bouton final, selon l'issue du combat. */
  readonly libelleFin = computed(() => {
    switch (this.statut()) {
      case 'DEFAITE':
        return 'REVENIR (1 PIÈCE PREMIUM)';
      default:
        return 'POURSUIVRE';
    }
  });

  /** Objets consommables en combat (catégorie OBJET, avec un effet défini). */
  readonly objetsConsommables = computed(() => {
    const catalogue = this.tousObjets();
    const inventaire = this.personnage()?.inventaire ?? [];
    return inventaire
      .filter((i) => i.categorie === 'OBJET' && i.quantite > 0)
      .map((i) => {
        const c = catalogue.find((o) => o.id.toLowerCase() === i.objetId.toLowerCase());
        const effetLabel = c && c.effets.length > 0 ? this.libelleEffet(c.effets[0]) : null;
        return { ...i, effetLabel, utilisable: !!effetLabel };
      })
      .filter((i) => i.utilisable);
  });

  private libelleEffet(effet: { type: string; valeur: number }): string {
    const libelle = effet.type === 'HABILETE' ? 'HABILETÉ' : 'ENDURANCE';
    return `+${effet.valeur} ${libelle}`;
  }

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

    this.objetService.lister().subscribe({
      next: (objets) => this.tousObjets.set(objets),
      error: (err) => console.error('Erreur lors du chargement des objets :', err),
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
          { txt: `HABILETÉ ${ennemi.habilite} — ENDURANCE ${ennemi.enduranceMax}.` },
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
      case 'DEFAITE':
        return 'Vous avez été vaincu lors de cet affrontement.';
      case 'FUITE':
        return 'Vous avez déjà fui ce combat.';
      default:
        return 'Ce combat est terminé.';
    }
  }

  /* --------------------------------------------------------------- actions */

  agir(action: ActionCombat): void {
    if (this.phase() !== 'MENU' || this.statut() !== 'EN_COURS' || this.actionEnCours()) return;

    if (action === 'OBJET') {
      this.sacOuvert.set(true);
      return;
    }

    if (action === 'FUITE' && !this.fuitePossible()) {
      this.jouerFile([{ txt: 'Impossible de rompre ce combat.' }]);
      return;
    }

    this.appellerTour(action);
  }

  choisirObjet(item: InventaireItem): void {
    this.sacOuvert.set(false);
    this.appellerTour('OBJET', item.objetId);
  }

  fermerSac(): void {
    this.sacOuvert.set(false);
  }

  private appellerTour(action: ActionCombat, objetId?: string): void {
    const id = this.personnageId();
    if (!id || this.actionEnCours()) return;

    this.actionEnCours.set(true);
    const ennemiAvant = this.ennemiActif();
    const enduranceJoueurAvant = this.enduranceJoueur();
    const nomObjetUtilise = objetId
      ? this.tousObjets().find((o) => o.id.toLowerCase() === objetId.toLowerCase())?.nom ?? objetId
      : null;

    this.combatService.jouerTour(id, action, objetId).subscribe({
      next: (c) => {
        this.actionEnCours.set(false);
        const messages = this.construireMessages(action, c, ennemiAvant, enduranceJoueurAvant, nomObjetUtilise);
        this.combat.set(c);
        // Recharge le personnage (endurance à jour, objet consommé retiré du sac).
        this.personnageService.recuperer(id).subscribe({
          next: (p) => this.personnage.set(p),
          error: (err) => console.error('Erreur lors du rechargement du personnage :', err),
        });
        this.jouerFile(messages);
      },
      error: (err) => {
        this.actionEnCours.set(false);
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
    nomObjetUtilise: string | null,
  ): Message[] {
    const tour = combat.dernierTour;
    const nomEnnemi = ennemiAvant?.nom ?? "l'ennemi";
    const messages: Message[] = [];

    if (!tour) {
      return messages;
    }

    if (action === 'FUITE') {
      messages.push({ txt: 'Vous rompez le combat et disparaissez dans les fougères.' });
      return messages;
    }

    if (action === 'ATTAQUE') {
      messages.push({ txt: `${this.nomJoueur()} porte son attaque !` });
    } else if (action === 'DEFENSE') {
      messages.push({ txt: `${this.nomJoueur()} lève sa garde.` });
    } else if (action === 'OBJET') {
      messages.push({ txt: `${this.nomJoueur()} utilise ${nomObjetUtilise ?? 'un objet'}.` });
    }

    if (tour.degatsInfliges !== null) {
      const degats = -tour.degatsInfliges;
      messages.push({
        txt: degats > 0 ? `${nomEnnemi} perd ${degats} points d'ENDURANCE.` : `${nomEnnemi} pare le coup. Aucun dégât.`,
        hit: degats > 0 ? 'ennemi' : null,
      });
    }

    const ennemiVaincu = ennemiAvant && combat.ennemis.find((e) => e.id === ennemiAvant.id)?.vaincu;
    if (action === 'ATTAQUE' && ennemiVaincu) {
      messages.push({ txt: `${nomEnnemi} s'effondre, vaincu.` });
    }

    if (tour.degatsSubis !== null) {
      const degats = -tour.degatsSubis;
      if (action === 'ATTAQUE') {
        messages.push({ txt: `${nomEnnemi} riposte !` });
      }
      let txt: string;
      if (degats <= 0) {
        txt = 'Vous esquivez le coup.';
      } else if (action === 'DEFENSE' && tour.reductionPourcent) {
        txt = `Vous perdez ${degats} points d'ENDURANCE (garde amortie de ${tour.reductionPourcent}%).`;
      } else {
        txt = `Vous perdez ${degats} points d'ENDURANCE.`;
      }
      messages.push({ txt, hit: degats > 0 ? 'joueur' : null });
    }

    if (action === 'DEFENSE' && tour.bonusHabiliteObtenu) {
      messages.push({
        txt: `Vous saisissez une ouverture : +${tour.bonusHabiliteObtenu} HABILETÉ pour votre prochaine attaque.`,
      });
    }

    if (combat.statut === 'DEFAITE') {
      messages.push({ txt: "Votre ENDURANCE tombe à zéro. Le voyage s'arrête ici." });
    } else if (combat.statut === 'INTERROMPU') {
      messages.push({ txt: 'Le combat est interrompu ; le récit continue.' });
    }

    return messages;
  }

  /** Touche sur la boîte de dialogue : termine la frappe, puis passe au message suivant. */
  avancer(event?: Event): void {
    if (event && (event.target as HTMLElement)?.closest('button')) return;
    if (this.phase() !== 'TEXTE') return;

    if (this.tapeEnCours()) {
      if (this.timerTape) clearInterval(this.timerTape);
      this.tape.set(this.msg().length);
      this.tapeEnCours.set(false);
      return;
    }
    this.prochain();
  }

  terminer(): void {
    const id = this.personnageId();
    if (!id) return;

    if (this.statut() === 'DEFAITE') {
      this.personnageService.revenirApresDefaite(id).subscribe({
        next: () => this.router.navigate(['/personnages', id, 'chapitre']),
        error: (err) => {
          console.error('Erreur lors du retour après défaite :', err);
          this.router.navigate(['/personnages', id, 'chapitre']);
        },
      });
      return;
    }

    this.router.navigate(['/personnages', id, 'chapitre']);
  }

  /* --------------------------------------------------------- file de messages */

  private jouerFile(file: Message[]): void {
    this.file = file.slice();
    this.phase.set('TEXTE');
    this.msg.set('');
    this.prochain();
  }

  private prochain(): void {
    if (!this.file.length) {
      const fin = this.statut() !== 'EN_COURS';
      this.phase.set(fin ? 'FIN' : 'MENU');
      if (!fin) this.msg.set('');
      return;
    }

    const m = this.file.shift()!;

    if (m.hit) {
      this.hit.set(m.hit);
      if (this.timerHit) clearTimeout(this.timerHit);
      this.timerHit = setTimeout(() => this.hit.set(null), 520);
    }

    this.phase.set('TEXTE');
    this.msg.set(m.txt);
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