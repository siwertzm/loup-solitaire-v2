import { Component, computed, inject, input, output } from '@angular/core';

import { EffetResponse } from '../../../../core/models/chapitre.model';
import { PersonnageResume } from '../../../../core/models/personnage.model';
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
}
