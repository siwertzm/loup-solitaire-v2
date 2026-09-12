import { Component, computed, effect, inject, input, output, signal } from '@angular/core';

import { ObjetChapResponse } from '../../../../core/models/chapitre.model';
import { ObjetResume, PersonnageResume } from '../../../../core/models/personnage.model';
import { ChapitreService } from '../../../../core/services/chapitre.service';
import { InventaireSheetService } from '../../../../core/services/inventaire-sheet.service';

/**
 * Contenu de l'onglet "objets" de l'écran chapitre : la liste des objets
 * proposés par le chapitre courant (avec ramassage) + le résumé de
 * l'inventaire du personnage (armes/objets+repas/bourse).
 *
 * Extrait de ChapitrePage pour garder ce fichier gérable : toute la logique
 * de ramassage (quantités restantes, appel API, décompte) est autonome ici.
 * Les données viennent du parent (qui reste seul propriétaire des appels
 * réseau /disciplines, /objets, /personnages/{id}) ; ce composant se
 * contente de les afficher et de notifier le parent après un ramassage
 * réussi via `ramasse`, pour qu'il mette à jour sa propre fiche personnage.
 *
 * Le détail complet de l'inventaire (feuille "SAC À DOS") est un composant
 * global partagé (shared/inventaire-sheet/), ouvrable depuis n'importe où
 * dans l'appli — ce composant se contente de déclencher son ouverture.
 */
@Component({
  selector: 'app-chapitre-objets',
  standalone: true,
  imports: [],
  templateUrl: './chapitre-objets.component.html',
  styleUrl: './chapitre-objets.component.scss',
})
export class ChapitreObjetsComponent {
  private readonly chapitreService = inject(ChapitreService);
  private readonly inventaireSheet = inject(InventaireSheetService);

  readonly objets = input.required<ObjetChapResponse[]>();
  readonly tousObjets = input.required<ObjetResume[]>();
  readonly personnage = input<PersonnageResume | null>(null);
  readonly personnageId = input<string | null>(null);

  /** Émis avec la fiche personnage à jour après un ramassage réussi. */
  readonly ramasse = output<PersonnageResume>();

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

  // Quantité restante à ramasser par objet optionnel (objetId -> valeur).
  // GET /chapitre renvoie la quantité proposée par le chapitre, pas ce qu'il
  // reste après ramassage : on la décrémente localement à chaque prise réussie.
  readonly restants = signal<Record<string, number>>({});
  readonly ramassageEnCours = signal<string | null>(null);

  constructor() {
    // Réinitialise les quantités restantes à chaque nouveau chapitre (l'input
    // `objets` change), remplaçant le reset manuel que faisait auparavant le
    // parent dans son abonnement à GET /chapitre.
    effect(() => {
      const init: Record<string, number> = {};
      this.objets().forEach((o) => (init[o.objetId] = o.valeur));
      this.restants.set(init);
    });
  }

  /**
   * Catégorie d'un objet (ARME/OBJET/OBJETS_SPECIAUX/REPAS/BOURSE), résolue
   * via le catalogue GET /objets : ObjetChapResponse (les objets d'un
   * chapitre) ne porte pas la catégorie, seulement objetId/nom/valeur/optionnel.
   */
  categorieObjet(objetId: string): string | null {
    return this.tousObjets().find((o) => o.id.toLowerCase() === objetId.toLowerCase())?.categorie ?? null;
  }

  /** Quantité restante à ramasser pour un objet optionnel du chapitre. */
  restant(objetId: string): number {
    return this.restants()[objetId] ?? 0;
  }

  /**
   * Ramasse 1 exemplaire d'un objet optionnel proposé par le chapitre.
   * Décrémente la quantité restante localement une fois le ramassage confirmé
   * par le backend, et notifie le parent (fiche personnage à jour).
   */
  prendreObjet(objet: { objetId: string; optionnel: boolean }): void {
    const id = this.personnageId();
    if (!id || !objet.optionnel) return;
    if (this.restant(objet.objetId) <= 0) return;
    if (this.ramassageEnCours()) return;

    this.ramassageEnCours.set(objet.objetId);
    this.chapitreService.ramasserObjet(id, objet.objetId).subscribe({
      next: (p) => {
        this.ramasse.emit(p);
        this.restants.update((r) => ({ ...r, [objet.objetId]: Math.max(0, this.restant(objet.objetId) - 1) }));
        this.ramassageEnCours.set(null);
      },
      error: (err) => {
        console.error("Erreur lors du ramassage de l'objet :", err);
        this.ramassageEnCours.set(null);
      },
    });
  }

  /** Ouvre la feuille "SAC À DOS" globale (voir shared/inventaire-sheet/). */
  ouvrirSac(): void {
    const id = this.personnageId();
    if (id) {
      this.inventaireSheet.ouvrir(id);
    }
  }
}