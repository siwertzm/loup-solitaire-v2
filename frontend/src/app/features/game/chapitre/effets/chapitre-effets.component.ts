import { Component, computed, inject, input, output, signal } from '@angular/core';

import { CondResponse, EffetResponse } from '../../../../core/models/chapitre.model';
import {
  DisciplineResume,
  InventaireItem,
  ObjetResume,
  PersonnageResume,
} from '../../../../core/models/personnage.model';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';
import { PersonnageService } from '../../../../core/services/personnage.service';

/**
 * Contenu de l'onglet "effets" de l'écran chapitre : la liste des effets
 * appliqués par le chapitre courant (REPAS, HABILETE, ENDURANCE, VOL, ECHANGE)
 * + le résumé de l'inventaire du personnage.
 *
 * Pour l'effet REPAS :
 * - Si le joueur possède la Discipline Kaï de la CHASSE, il est dispensé de repas.
 * - Sinon, 1 repas est automatiquement consommé (ou -3 ENDURANCE si aucun repas).
 */
@Component({
  selector: 'app-chapitre-effets',
  standalone: true,
  imports: [],
  templateUrl: './chapitre-effets.component.html',
  styleUrl: './chapitre-effets.component.scss',
})
export class ChapitreEffetsComponent {
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly personnageService = inject(PersonnageService);

  readonly effets = input.required<EffetResponse[]>();
  readonly personnage = input<PersonnageResume | null>(null);
  readonly personnageId = input<string | null>(null);

  // Catalogues complets, uniquement pour résoudre les noms lisibles des
  // DISCIPLINE/OBJET ciblés par une condition d'effet (ex. "bouclier_psychique"
  // -> "Bouclier Psychique"). Transmis par ChapitrePage, qui les charge déjà.
  readonly toutesDisciplines = input<DisciplineResume[]>([]);
  readonly tousObjets = input<ObjetResume[]>([]);

  /** Émis avec la fiche personnage à jour si un effet modifie l'état. */
  readonly effetTraite = output<PersonnageResume>();

  readonly inventaire = computed(() => this.personnage()?.inventaire ?? []);
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);

  /** Indique si le personnage maîtrise la discipline Kaï de la Chasse. */
  readonly aDisciplineChasse = computed(() =>
    this.disciplines().some((d) => d.toUpperCase() === 'CHASSE'),
  );

  /** Statut du résultat de l'effet repas ('CHASSE', 'REPAS_CONSOMME', 'MALUS_ENDURANCE'). */
  statutRepas(effet: EffetResponse): string {
    if (effet.resultat) {
      return effet.resultat;
    }
    if (this.aDisciplineChasse()) {
      return 'CHASSE';
    }
    return this.repasCount() > 0 ? 'REPAS_CONSOMME' : 'MALUS_ENDURANCE';
  }

  // Alignés sur InventaireService (backend) :
  // MAX_ARMES=2, MAX_OBJETS_ET_REPAS=8 (partagé entre OBJET et REPAS), MAX_BOURSE=50.
  readonly maxArmes = 2;
  readonly maxObjetsEtRepas = 8;
  readonly maxBourse = 50;

  readonly armesCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'ARME')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly objetsCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'OBJET')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly repasCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'REPAS')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly objetsEtRepasCount = computed(() => this.objetsCount() + this.repasCount());

  readonly bourseCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'BOURSE')
      .reduce((total, i) => total + i.quantite, 0),
  );

  /** Ouvre la feuille "SAC À DOS" globale (voir shared/inventaire-sheet/). */
  ouvrirSac(): void {
    const id = this.personnageId();
    if (id) {
      this.inventaireSheet.ouvrir(id);
    }
  }

  // --- Effet HABILETE avec condition DISCIPLINE ou OBJET ---------------
  // Sémantique INVERSÉE côté backend (voir EffetChapitreService) : le
  // malus ne s'applique que si le personnage NE POSSÈDE PAS la discipline/
  // l'objet ciblé. Comme ChapitreMapper.estEffetActif ne filtre que sur les
  // conditions HASARD, ces effets sont TOUJOURS renvoyés par l'API, qu'ils
  // aient réellement été appliqués ou non — d'où le badge "ÉVITÉ" ici pour
  // ne pas laisser croire à un malus qui n'a pas eu lieu.

  /** Unique condition DISCIPLINE ou OBJET d'un effet HABILETE, s'il y en a une. */
  conditionDisciplineOuObjet(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length == 0) {
      return null;
    }
    const condition = effet.conditions[0];
    return condition.type === 'DISCIPLINE' || condition.type === 'OBJET' || condition.type === 'PERMANENT' ? condition : null;
  }

  /** Unique condition ASSAUT_MAX d'un effet HABILETE, s'il y en a une (ex.
   * chapitre 283 : bonus/malus limité aux N premiers assauts du combat).
   * Calculé et appliqué EN TEMPS RÉEL par CombatService selon
   * Combat.assautsLivres — jamais figé à l'arrivée sur le chapitre comme
   * les autres conditions HABILETE, donc aucun état "évité" à afficher ici. */
  conditionAssautMax(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length !== 1) {
      return null;
    }
    const condition = effet.conditions[0];
    return condition.type === 'ASSAUT_MAX' ? condition : null;
  }

  /** Libellé humain pour une condition ASSAUT_MAX ("1" -> "Premier assaut"). */
  libelleAssautMax(condition: CondResponse): string {
    const n = Number(condition.valeur);
    if (n === 1) {
      return 'Premier assaut';
    }
    return Number.isFinite(n) ? `${n} premiers assauts` : 'Assaut limité';
  }

  /** true si le malus a été évité (discipline/objet requis bien possédé). */
  effetEvite(condition: CondResponse): boolean {
    if (!condition.targetId) {
      return false;
    }
    return condition.type === 'DISCIPLINE'
      ? this.possedeDiscipline(condition.targetId)
      : this.possedeObjet(condition.targetId);
  }

  private possedeDiscipline(targetId: string): boolean {
    return this.disciplines().some((d) => d.toUpperCase() === targetId.toUpperCase());
  }

  private possedeObjet(targetId: string): boolean {
    return this.inventaire().some(
      (i) => i.objetId.toLowerCase() === targetId.toLowerCase() && i.quantite > 0,
    );
  }

  /** Nom lisible d'une discipline ("bouclier_psychique" -> "Bouclier Psychique"). */
  nomDiscipline(targetId: string): string {
    const discipline = this.toutesDisciplines().find(
      (d) => d.id.toLowerCase() === targetId.toLowerCase(),
    );
    return discipline?.nom ?? targetId;
  }

  /** Nom lisible d'un objet ("torche" -> "Torche"). */
  nomObjet(targetId: string): string {
    const objet = this.tousObjets().find((o) => o.id.toLowerCase() === targetId.toLowerCase());
    return objet?.nom ?? targetId;
  }

  // --- Effet VOL ---------------------------------------------------------
  // La "valeur" code une PORTÉE de perte, pas une quantité (voir
  // EffetChapitreService, backend) :
  //   10 -> tout (sac + armes) ; 8 -> tout le sac ; 2+cond ARME -> toutes
  //   les armes : ces 3 cas sont résolus AUTOMATIQUEMENT par le backend à
  //   l'arrivée sur le chapitre, rien à faire ici à part informer.
  //   1 (+cond ARME ou non) -> "au choix" : pose Personnage.volEnAttente
  //   ("ARME" ou "TOUT"), à résoudre via POST /vol/{objetId} avant de
  //   pouvoir continuer.

  /** true si la portée du vol est restreinte aux armes (condition ARME présente). */
  private volPorteeArme(effet: EffetResponse): boolean {
    return effet.conditions.some((c) => c.type === 'ARME');
  }

  /** true si ce vol nécessite un choix du joueur (valeur=1). */
  volInteractif(effet: EffetResponse): boolean {
    return effet.valeur === 1;
  }

  /** true si CE vol précis est celui actuellement en attente de résolution. */
  volEnAttentePourEffet(effet: EffetResponse): boolean {
    if (!this.volInteractif(effet)) {
      return false;
    }
    const attente = this.personnage()?.volEnAttente;
    return attente === (this.volPorteeArme(effet) ? 'ARME' : 'TOUT');
  }

  /** Description humaine du vol, quelle que soit sa portée. */
  libelleVol(effet: EffetResponse): string {
    if (this.volInteractif(effet)) {
      return this.volPorteeArme(effet)
        ? 'Choisissez l\u2019arme que vous perdez'
        : 'Choisissez l\u2019objet que vous perdez';
    }
    if (effet.valeur === 10) {
      return 'Sac et armes';
    }
    if (effet.valeur === 8) {
      return 'Sac à dos';
    }
    if (this.volPorteeArme(effet)) {
      return 'Vous perdez toutes vos armes';
    }
    return 'Vol';
  }

  /** Objets/armes éligibles à la perte pour CE vol (portée ARME ou TOUT). */
  itemsEligiblesVol(effet: EffetResponse): InventaireItem[] {
    const categoriesTout: InventaireItem['categorie'][] = ['ARME', 'OBJET', 'REPAS'];
    return this.inventaire().filter((i) =>
      i.quantite > 0 && (this.volPorteeArme(effet) ? i.categorie === 'ARME' : categoriesTout.includes(i.categorie)),
    );
  }

  /** Effet VOL actuellement affiché dans le popup de choix (null = fermé). */
  readonly volPopupEffet = signal<EffetResponse | null>(null);
  readonly volEnCours = signal<string | null>(null);

  ouvrirChoixVol(effet: EffetResponse): void {
    this.volPopupEffet.set(effet);
  }

  fermerChoixVol(): void {
    this.volPopupEffet.set(null);
  }

  /** Confirme la perte de l'objet choisi (POST /personnages/{id}/vol/{objetId}). */
  choisirObjetVole(objetId: string): void {
    const id = this.personnageId();
    if (!id || this.volEnCours()) return;

    this.volEnCours.set(objetId);
    this.personnageService.resoudreVol(id, objetId).subscribe({
      next: (p) => {
        this.effetTraite.emit(p);
        this.inventaireSheet.notifierMiseAJour(p);
        this.volEnCours.set(null);
        this.fermerChoixVol();
      },
      error: (err) => {
        console.error('Erreur lors de la résolution du vol :', err);
        this.volEnCours.set(null);
      },
    });
  }

  // --- Effet ECHANGE -----------------------------------------------------
  // Cas unique dans ce tome (chapitre 307) : un objet précis est proposé
  // (targetId de l'unique condition de l'effet), à condition d'échanger
  // contre un objet DÉJÀ possédé de la MÊME catégorie (vérifié aussi côté
  // backend, voir PersonnageService.echangerObjet). L'effet est toujours
  // renvoyé par l'API (non filtré, comme VOL/HABILETE) : on détecte que
  // l'échange a déjà eu lieu en vérifiant que l'objet proposé est possédé.

  /** Unique condition (ARME/OBJET) d'un effet ECHANGE : cible l'objet proposé. */
  conditionEchange(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length !== 1) {
      return null;
    }
    return effet.conditions[0];
  }

  /** true si l'objet proposé est déjà dans l'inventaire (échange déjà fait). */
  echangeDejaFait(condition: CondResponse): boolean {
    if (!condition.targetId) {
      return false;
    }
    return this.inventaire().some(
      (i) => i.objetId.toLowerCase() === condition.targetId!.toLowerCase() && i.quantite > 0,
    );
  }

  /** Catégorie de l'objet proposé, résolue via le catalogue (GET /objets). */
  private categorieObjetPropose(targetId: string): string | null {
    return this.tousObjets().find((o) => o.id.toLowerCase() === targetId.toLowerCase())?.categorie ?? null;
  }

  /** Objets déjà possédés de la même catégorie que l'objet proposé (à céder en échange). */
  itemsEligiblesEchange(condition: CondResponse): InventaireItem[] {
    if (!condition.targetId) {
      return [];
    }
    const categorie = this.categorieObjetPropose(condition.targetId);
    return this.inventaire().filter((i) => i.categorie === categorie && i.quantite > 0);
  }

  /** Effet ECHANGE actuellement affiché dans le popup de choix (null = fermé). */
  readonly echangePopupEffet = signal<EffetResponse | null>(null);
  readonly echangeEnCours = signal<string | null>(null);

  ouvrirEchange(effet: EffetResponse): void {
    this.echangePopupEffet.set(effet);
  }

  fermerEchange(): void {
    this.echangePopupEffet.set(null);
  }

  /** Confirme l'échange : objetARetirerId cédé contre l'objet proposé. */
  confirmerEchange(objetAAjouterId: string, objetARetirerId: string): void {
    const id = this.personnageId();
    if (!id || this.echangeEnCours()) return;

    this.echangeEnCours.set(objetARetirerId);
    this.personnageService.echangerObjet(id, objetAAjouterId, objetARetirerId).subscribe({
      next: (p) => {
        this.effetTraite.emit(p);
        this.inventaireSheet.notifierMiseAJour(p);
        this.echangeEnCours.set(null);
        this.fermerEchange();
      },
      error: (err) => {
        console.error("Erreur lors de l'échange d'objet :", err);
        this.echangeEnCours.set(null);
      },
    });
  }
}