import { Component, computed, inject, input, output } from '@angular/core';

import { CondResponse, EffetResponse } from '../../../../core/models/chapitre.model';
import { DisciplineResume, ObjetResume, PersonnageResume } from '../../../../core/models/personnage.model';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';

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
    if (effet.conditions.length !== 1) {
      return null;
    }
    const condition = effet.conditions[0];
    return condition.type === 'DISCIPLINE' || condition.type === 'OBJET' ? condition : null;
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
}