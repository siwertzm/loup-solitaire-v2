import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { IonItemSliding, IonItem, IonItemOptions, IonItemOption } from '@ionic/angular';

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
  imports: [IonItemSliding, IonItem, IonItemOptions, IonItemOption],
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

  // Aligné sur InventaireService.MALUS_SANS_ARME (backend) : sans arme du
  // tout, l'HABILETÉ effective est habiliteBase - 4 ("Main nue").
  readonly malusSansArme = -4;
  readonly aucuneArme = computed(() => this.armesDetail().length === 0);

  readonly objetsEtRepasDetail = computed(() => {
    const items = this.inventaire().filter((i) => i.categorie === 'OBJET' || i.categorie === 'REPAS');
    return items.map((i) => {
      const catalogue = this.tousObjets().find((o) => o.id.toLowerCase() === i.objetId.toLowerCase());
      // Consommable : catégorie OBJET avec au moins un effet défini (ex.
      // Potion de guérison, Laumspur, Essence d'Alether) — les REPAS et les
      // OBJET sans effet (torche, message, savon...) ne le sont pas.
      const consommable = catalogue?.categorie === 'OBJET' && catalogue.effets.length > 0;
      const effetLabel = consommable ? this.libelleEffet(catalogue!.effets[0]) : null;
      return { ...i, consommable, effetLabel, description: catalogue?.description ?? null };
    });
  });

  private libelleEffet(effet: { type: string; valeur: number }): string {
    const libelle = effet.type === 'HABILETE' ? 'HABILETÉ' : 'ENDURANCE';
    return `+${effet.valeur} ${libelle}`;
  }

  readonly objetsSpeciauxDetail = computed(() =>
    this.inventaire().filter((i) => i.categorie === 'OBJETS_SPECIAUX'),
  );

  readonly retraitEnCours = signal<string | null>(null);
  readonly consommationEnCours = signal<string | null>(null);
  private readonly feuilleSac = viewChild<ElementRef<HTMLElement>>('feuilleSac');

  /** Objet en attente de confirmation dans le popup "consommer" ; null = fermé. */
  readonly objetAConfirmer = signal<{
    objetId: string;
    nom: string;
    description: string | null;
    effetLabel: string | null;
    sliding: IonItemSliding;
  } | null>(null);

  /** Ouvre le popup de confirmation (n'appelle pas encore l'API). */
  demanderConsommation(objet: {
    objetId: string;
    nom: string;
    description: string | null;
    effetLabel: string | null;
  }, sliding: IonItemSliding): void {
    this.objetAConfirmer.set({ ...objet, sliding });
  }

  annulerConsommation(): void {
    this.objetAConfirmer.set(null);
  }

  /**
   * Consomme 1 exemplaire (potion, Laumspur, essence d'Alether...) :
   * applique l'effet PUIS retire l'objet — POST /objets/{objetId}/consommer
   * fait les deux côté backend en une seule transaction.
   */
  confirmerConsommation(): void {
    const attente = this.objetAConfirmer();
    const id = this.sheet.personnageId();
    if (!attente || !id || this.consommationEnCours()) return;

    this.consommationEnCours.set(attente.objetId);
    this.personnageService.consommerObjet(id, attente.objetId).subscribe({
      next: (p) => {
        this.personnage.set(p);
        this.consommationEnCours.set(null);
        this.sheet.notifierMiseAJour(p);
        attente.sliding.close();
        this.objetAConfirmer.set(null);
      },
      error: (err) => {
        console.error("Erreur lors de la consommation de l'objet :", err);
        this.consommationEnCours.set(null);
      },
    });
  }

  /**
   * Retire 1 exemplaire d'un objet possédé (DELETE /personnages/{id}/objets/{objetId}).
   * Recalcule l'HABILETÉ côté backend si c'était une arme — la réponse à jour
   * remplace directement `personnage`, tous les compteurs se recalculent seuls.
   *
   * `sliding` : référence du ion-item-sliding concerné, pour le refermer
   * après coup (sinon le bouton "Retirer" reste affiché, ouvert, même une
   * fois l'objet retiré) ; on remonte aussi le scroll en haut de la feuille.
   */
  retirer(objetId: string, sliding: IonItemSliding): void {
    const id = this.sheet.personnageId();
    if (!id || this.retraitEnCours()) return;

    this.retraitEnCours.set(objetId);
    this.personnageService.retirerObjet(id, objetId, 1).subscribe({
      next: (p) => {
        this.personnage.set(p);
        this.retraitEnCours.set(null);
        this.sheet.notifierMiseAJour(p);
        sliding.close();
        this.feuilleSac()?.nativeElement.scrollTo({ top: 0 });
      },
      error: (err) => {
        console.error("Erreur lors du retrait de l'objet :", err);
        this.retraitEnCours.set(null);
      },
    });
  }

  fermer(): void {
    this.sheet.fermer();
  }
}