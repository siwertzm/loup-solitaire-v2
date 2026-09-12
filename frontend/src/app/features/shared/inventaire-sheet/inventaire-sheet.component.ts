import { Component, computed, effect, inject, signal } from '@angular/core';

import { PersonnageResume, ObjetResume } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { ObjetService } from '../../../core/services/objet.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';

/**
 * Feuille "SAC À DOS" — détail de l'inventaire d'un personnage (armes,
 * objets & repas, objets spéciaux, bourse), ouvrable depuis n'importe où
 * dans l'appli via InventaireSheetService.ouvrir(personnageId).
 *
 * Autonome : ne reçoit que l'id du personnage (via le service), et va
 * chercher lui-même sa fiche + le catalogue d'objets à chaque ouverture.
 * Monté une seule fois à la racine (voir app.ts), à côté du router-outlet.
 */
@Component({
  selector: 'app-inventaire-sheet',
  standalone: true,
  imports: [],
  templateUrl: './inventaire-sheet.component.html',
  styleUrl: './inventaire-sheet.component.scss',
})
export class InventaireSheetComponent {
  private readonly sheet = inject(InventaireSheetService);
  private readonly personnageService = inject(PersonnageService);
  private readonly objetService = inject(ObjetService);

  readonly ouvert = computed(() => this.sheet.personnageId() !== null);

  readonly personnage = signal<PersonnageResume | null>(null);
  readonly tousObjets = signal<ObjetResume[]>([]);
  readonly chargement = signal(false);

  constructor() {
    // Recharge à chaque ouverture (personnageId passe de null à une valeur,
    // ou change directement si le service est rouvert sur un autre personnage).
    effect(() => {
      const id = this.sheet.personnageId();
      if (!id) return;

      this.chargement.set(true);
      this.personnageService.recuperer(id).subscribe({
        next: (p) => {
          this.personnage.set(p);
          this.chargement.set(false);
        },
        error: (err) => {
          console.error("Erreur lors du chargement de l'inventaire :", err);
          this.chargement.set(false);
        },
      });

      // Catalogue rechargé aussi à chaque ouverture, coût négligeable et ça
      // évite toute question de fraîcheur des données entre deux usages.
      this.objetService.lister().subscribe({
        next: (objets) => this.tousObjets.set(objets),
        error: (err) => console.error('Erreur lors du chargement des objets :', err),
      });
    });
  }

  readonly inventaire = computed(() => this.personnage()?.inventaire ?? []);

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

  readonly objetsEtRepasCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'OBJET' || i.categorie === 'REPAS')
      .reduce((total, i) => total + i.quantite, 0),
  );

  readonly bourseCount = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'BOURSE')
      .reduce((total, i) => total + i.quantite, 0),
  );

  // "MAÎTRISÉE" : l'arme qui correspond à armeMaitrisee (Discipline Maîtrise
  // des Armes) donne +2 HABILETÉ tant qu'elle est possédée — aligné sur
  // InventaireService.BONUS_ARME_MAITRISEE (backend). Toutes les autres
  // armes possédées sont simplement "ÉQUIPÉE" (pas de vraie notion
  // d'emplacement équipé/au sac côté backend, juste "possédée").
  readonly bonusArmeMaitrisee = 2;

  readonly armesDetail = computed(() => {
    const armes = this.inventaire().filter((i) => i.categorie === 'ARME');
    const maitriseeNom = this.personnage()?.armeMaitrisee ?? null;
    return armes.map((a) => ({ ...a, maitrisee: maitriseeNom !== null && a.nom === maitriseeNom }));
  });

  readonly objetsEtRepasDetail = computed(() =>
    this.inventaire().filter((i) => i.categorie === 'OBJET' || i.categorie === 'REPAS'),
  );

  readonly objetsSpeciauxDetail = computed(() =>
    this.inventaire().filter((i) => i.categorie === 'OBJETS_SPECIAUX'),
  );

  fermer(): void {
    this.sheet.fermer();
  }
}