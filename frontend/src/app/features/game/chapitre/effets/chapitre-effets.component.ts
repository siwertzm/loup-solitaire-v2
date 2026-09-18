import { Component, computed, inject, input, output, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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
 * Contenu de l'onglet "effets" de l'écran chapitre :
 * - REPAS
 * - HABILITE
 * - ENDURANCE
 * - VOL
 * - ECHANGE
 *
 * + résumé de l'inventaire du personnage.
 */
@Component({
  selector: 'app-chapitre-effets',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './chapitre-effets.component.html',
  styleUrl: './chapitre-effets.component.scss',
})
export class ChapitreEffetsComponent {
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly personnageService = inject(PersonnageService);
  private readonly translate = inject(TranslateService);

  readonly effets = input.required<EffetResponse[]>();
  readonly personnage = input<PersonnageResume | null>(null);
  readonly personnageId = input<string | null>(null);

  // Catalogues complets utilisés pour résoudre les noms lisibles
  // des disciplines et objets.
  readonly toutesDisciplines = input<DisciplineResume[]>([]);
  readonly tousObjets = input<ObjetResume[]>([]);

  /** Émis avec la fiche personnage à jour si un effet modifie l'état. */
  readonly effetTraite = output<PersonnageResume>();

  readonly inventaire = computed(() => this.personnage()?.inventaire ?? []);

  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);

  // ============================================================
  // REPAS
  // ============================================================

  /** Indique si le personnage maîtrise la Discipline Kaï de la Chasse. */
  readonly aDisciplineChasse = computed(() =>
    this.disciplines().some((d) => d.toUpperCase() === 'CHASSE'),
  );

  readonly bonusArmeMaitrisee = 2;

  /**
   * TRUE si l'item est l'arme maîtrisée du personnage.
   * Même logique que dans InventaireSheetComponent.
   */
  armeMaitrisee(item: InventaireItem): boolean {
    if (item.categorie !== 'ARME') {
      return false;
    }

    const maitriseeNom = this.personnage()?.armeMaitrisee ?? null;

    return maitriseeNom !== null && item.nom === maitriseeNom;
  }

  /**
   * Statut du résultat de l'effet repas :
   * - CHASSE
   * - REPAS_CONSOMME
   * - MALUS_ENDURANCE
   */
  statutRepas(effet: EffetResponse): string {
    if (effet.resultat) {
      return effet.resultat;
    }

    if (this.aDisciplineChasse()) {
      return 'CHASSE';
    }

    return this.repasCount() > 0 ? 'REPAS_CONSOMME' : 'MALUS_ENDURANCE';
  }

  // ============================================================
  // INVENTAIRE
  // ============================================================

  // Alignés sur InventaireService côté backend.
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

  /** Ouvre la feuille globale du sac à dos. */
  ouvrirSac(): void {
    const id = this.personnageId();

    if (id) {
      this.inventaireSheet.ouvrir(id);
    }
  }

  // ============================================================
  // HABILITE
  // ============================================================

  /**
   * Retourne une condition permettant au HTML de reconnaître
   * un effet HABILITE conditionnel.
   *
   * IMPORTANT :
   * Un effet peut maintenant posséder PLUSIEURS conditions
   * OBJET / DISCIPLINE.
   *
   * Exemple chapitre 170 :
   *
   * Torche + Briquet d'Amadou.
   *
   * On retourne toujours la première condition uniquement pour
   * conserver la compatibilité avec le HTML existant.
   *
   * La vérification réelle de TOUTES les conditions est effectuée
   * dans effetEvite().
   */
  conditionDisciplineOuObjet(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length === 0) {
      return null;
    }

    /*
     * PERMANENT est actuellement une condition unique.
     */
    if (effet.conditions.length === 1 && effet.conditions[0].type === 'PERMANENT') {
      return effet.conditions[0];
    }

    /*
     * Vérifie que toutes les conditions sont des protections
     * OBJET ou DISCIPLINE.
     */
    const uniquementProtections = effet.conditions.every(
      (condition) => condition.type === 'DISCIPLINE' || condition.type === 'OBJET',
    );

    if (!uniquementProtections) {
      return null;
    }

    /*
     * Le HTML actuel attend une condition.
     * On renvoie la première mais effetEvite() retrouvera ensuite
     * l'effet complet pour vérifier TOUTES les protections.
     */
    return effet.conditions[0];
  }

  /**
   * Condition ASSAUT_MAX d'un effet HABILITE.
   *
   * Exemple chapitre 283 :
   * bonus limité au premier assaut.
   */
  conditionAssautMax(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length !== 1) {
      return null;
    }

    const condition = effet.conditions[0];

    return condition.type === 'ASSAUT_MAX' ? condition : null;
  }

  /**
   * Libellé humain d'une condition ASSAUT_MAX.
   */
  libelleAssautMax(condition: CondResponse): string {
    const n = Number(condition.valeur);

    if (n === 1) {
      return this.translate.instant('CHAPITRE_EFFETS.ASSAUT_PREMIER');
    }

    return Number.isFinite(n)
      ? this.translate.instant('CHAPITRE_EFFETS.ASSAUT_N_PREMIERS', { n })
      : this.translate.instant('CHAPITRE_EFFETS.ASSAUT_LIMITE');
  }

  /**
   * TRUE si le malus HABILITE a été évité.
   *
   * IMPORTANT :
   *
   * On ne vérifie plus uniquement la condition reçue.
   *
   * On retrouve l'effet auquel appartient cette condition,
   * puis on vérifie TOUTES ses conditions OBJET / DISCIPLINE.
   *
   * Le malus est évité uniquement si TOUTES les protections
   * demandées sont possédées.
   *
   * Chapitre 170 :
   *
   * Torche + Briquet -> true
   * Torche seule -> false
   * Briquet seul -> false
   * Aucun -> false
   */
  effetEvite(condition: CondResponse): boolean {
    /*
     * On retrouve l'effet contenant cette condition.
     *
     * Comme conditionDisciplineOuObjet() retourne directement
     * l'objet présent dans effet.conditions, includes() fonctionne
     * avec la même référence.
     */
    const effet = this.effets().find((e) => e.conditions.includes(condition));

    /*
     * Sécurité :
     * si l'effet n'est pas retrouvé, on retombe sur la logique
     * d'une condition unique.
     */
    if (!effet) {
      return this.conditionProtectionPossedee(condition);
    }

    /*
     * On récupère uniquement les protections contre le malus.
     */
    const protections = effet.conditions.filter(
      (c) => c.type === 'DISCIPLINE' || c.type === 'OBJET',
    );

    if (protections.length === 0) {
      return false;
    }

    /*
     * Toutes les protections doivent être satisfaites.
     */
    return protections.every((c) => this.conditionProtectionPossedee(c));
  }

  /**
   * Vérifie une protection individuelle.
   */
  private conditionProtectionPossedee(condition: CondResponse): boolean {
    if (!condition.targetId) {
      return false;
    }

    if (condition.type === 'DISCIPLINE') {
      return this.possedeDiscipline(condition.targetId);
    }

    if (condition.type === 'OBJET') {
      return this.possedeObjet(condition.targetId);
    }

    return false;
  }

  /**
   * Vérifie si le personnage possède une discipline.
   */
  private possedeDiscipline(targetId: string): boolean {
    return this.disciplines().some((d) => d.toUpperCase() === targetId.toUpperCase());
  }

  /**
   * Vérifie si le personnage possède au moins un exemplaire
   * de l'objet demandé.
   */
  private possedeObjet(targetId: string): boolean {
    return this.inventaire().some(
      (i) => i.objetId.toLowerCase() === targetId.toLowerCase() && i.quantite > 0,
    );
  }

  /**
   * Nom lisible d'une discipline.
   *
   * Exemple :
   * bouclier_psychique
   * ->
   * Bouclier Psychique
   */
  nomDiscipline(targetId: string): string {
    const discipline = this.toutesDisciplines().find(
      (d) => d.id.toLowerCase() === targetId.toLowerCase(),
    );

    return discipline?.nom ?? targetId;
  }

  /**
   * Nom lisible d'un objet.
   *
   * Exemple :
   * torche
   * ->
   * Torche
   */
  nomObjet(targetId: string): string {
    const objet = this.tousObjets().find((o) => o.id.toLowerCase() === targetId.toLowerCase());

    return objet?.nom ?? targetId;
  }

  // ============================================================
  // VOL
  // ============================================================

  /**
   * La valeur code une PORTÉE de perte :
   *
   * 10 -> tout
   * 8  -> tout le sac
   * 2 + condition ARME -> toutes les armes
   * 1 + condition ARME -> une arme au choix
   * 1 sans condition -> un objet/repas/arme au choix
   */

  /**
   * TRUE si le vol est limité aux armes.
   */
  private volPorteeArme(effet: EffetResponse): boolean {
    return effet.conditions.some((c) => c.type === 'ARME');
  }

  /**
   * TRUE si le joueur doit choisir l'objet volé.
   */
  volInteractif(effet: EffetResponse): boolean {
    return effet.valeur === 1;
  }

  /**
   * TRUE si ce vol est actuellement en attente.
   */
  volEnAttentePourEffet(effet: EffetResponse): boolean {
    if (!this.volInteractif(effet)) {
      return false;
    }

    const attente = this.personnage()?.volEnAttente;

    return attente === (this.volPorteeArme(effet) ? 'ARME' : 'TOUT');
  }

  /**
   * Description humaine du vol.
   */
  libelleVol(effet: EffetResponse): string {
    if (this.volInteractif(effet)) {
      return this.volPorteeArme(effet)
        ? this.translate.instant('CHAPITRE_EFFETS.VOL_CHOIX_ARME')
        : this.translate.instant('CHAPITRE_EFFETS.VOL_CHOIX_OBJET');
    }

    if (effet.valeur === 10) {
      return this.translate.instant('CHAPITRE_EFFETS.VOL_SAC_ET_ARMES');
    }

    if (effet.valeur === 8) {
      return this.translate.instant('CHAPITRE_EFFETS.VOL_SAC_A_DOS');
    }

    if (this.volPorteeArme(effet)) {
      return this.translate.instant('CHAPITRE_EFFETS.VOL_TOUTES_ARMES');
    }

    return this.translate.instant('CHAPITRE_EFFETS.VOL_GENERIQUE');
  }

  /**
   * Objets éligibles pour le vol.
   */
  itemsEligiblesVol(effet: EffetResponse): InventaireItem[] {
    const categoriesTout: InventaireItem['categorie'][] = ['ARME', 'OBJET', 'REPAS'];

    return this.inventaire().filter(
      (i) =>
        i.quantite > 0 &&
        (this.volPorteeArme(effet) ? i.categorie === 'ARME' : categoriesTout.includes(i.categorie)),
    );
  }

  /**
   * Effet VOL actuellement affiché dans le popup.
   */
  readonly volPopupEffet = signal<EffetResponse | null>(null);

  readonly volEnCours = signal<string | null>(null);

  ouvrirChoixVol(effet: EffetResponse): void {
    this.volPopupEffet.set(effet);
  }

  fermerChoixVol(): void {
    this.volPopupEffet.set(null);
  }

  /**
   * Confirme la perte de l'objet choisi.
   */
  choisirObjetVole(objetId: string): void {
    const id = this.personnageId();

    if (!id || this.volEnCours()) {
      return;
    }

    this.volEnCours.set(objetId);

    this.personnageService.resoudreVol(id, objetId).subscribe({
      next: (p) => {
        this.effetTraite.emit(p);

        this.inventaireSheet.notifierMiseAJour(p);

        this.volEnCours.set(null);

        this.fermerChoixVol();
      },

      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_EFFETS.ERREUR_VOL'), err);

        this.volEnCours.set(null);
      },
    });
  }

  // ============================================================
  // ECHANGE
  // ============================================================

  /**
   * Cas unique dans ce tome :
   * chapitre 307.
   *
   * Un objet précis est proposé contre un objet déjà possédé
   * de la même catégorie.
   */

  /**
   * Unique condition d'un effet ECHANGE.
   */
  conditionEchange(effet: EffetResponse): CondResponse | null {
    if (effet.conditions.length !== 1) {
      return null;
    }

    return effet.conditions[0];
  }

  /**
   * TRUE si l'objet proposé est déjà possédé.
   */
  echangeDejaFait(condition: CondResponse): boolean {
    if (!condition.targetId) {
      return false;
    }

    return this.inventaire().some(
      (i) => i.objetId.toLowerCase() === condition.targetId!.toLowerCase() && i.quantite > 0,
    );
  }

  /**
   * Catégorie de l'objet proposé.
   */
  private categorieObjetPropose(targetId: string): string | null {
    return (
      this.tousObjets().find((o) => o.id.toLowerCase() === targetId.toLowerCase())?.categorie ??
      null
    );
  }

  /**
   * Objets déjà possédés pouvant être échangés.
   */
  itemsEligiblesEchange(condition: CondResponse): InventaireItem[] {
    if (!condition.targetId) {
      return [];
    }

    const categorie = this.categorieObjetPropose(condition.targetId);

    return this.inventaire().filter((i) => i.categorie === categorie && i.quantite > 0);
  }

  /**
   * Effet ECHANGE actuellement affiché.
   */
  readonly echangePopupEffet = signal<EffetResponse | null>(null);

  readonly echangeEnCours = signal<string | null>(null);

  ouvrirEchange(effet: EffetResponse): void {
    this.echangePopupEffet.set(effet);
  }

  fermerEchange(): void {
    this.echangePopupEffet.set(null);
  }

  /**
   * Confirme l'échange.
   */
  confirmerEchange(objetAAjouterId: string, objetARetirerId: string): void {
    const id = this.personnageId();

    if (!id || this.echangeEnCours()) {
      return;
    }

    this.echangeEnCours.set(objetARetirerId);

    this.personnageService.echangerObjet(id, objetAAjouterId, objetARetirerId).subscribe({
      next: (p) => {
        this.effetTraite.emit(p);

        this.inventaireSheet.notifierMiseAJour(p);

        this.echangeEnCours.set(null);

        this.fermerEchange();
      },

      error: (err) => {
        console.error(this.translate.instant('CHAPITRE_EFFETS.ERREUR_ECHANGE'), err);

        this.echangeEnCours.set(null);
      },
    });
  }

  titreVol(effet: EffetResponse): string {
    switch (effet.nom?.toUpperCase()) {
      case 'CASSE':
        return this.translate.instant('CHAPITRE_EFFETS.CASSE_TITRE');

      case 'PERTE':
        return this.translate.instant('CHAPITRE_EFFETS.PERTE_TITRE');

      case 'VOL':
      default:
        return this.translate.instant('CHAPITRE_EFFETS.VOL_TITRE');
    }
  }
}
