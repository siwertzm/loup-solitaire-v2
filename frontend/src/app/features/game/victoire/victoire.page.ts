import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { PersonnageResume } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';

/**
 * Écran de fin du tome 1, atteint depuis ChapitrePage quand le chapitre
 * courant n'a plus aucun lien (chapitre 350 : son seul lien, vers la page
 * "351", est filtré au chargement des données comme "352" pour la mort
 * narrative — voir GameDataLoader.PAGES_FIN_DE_JEU côté backend). Contenu
 * statique, à l'image des pages "regle/*" : rien à valider côté serveur,
 * l'aventure s'arrête ici pour ce tome.
 */
@Component({
  selector: 'app-victoire',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './victoire.page.html',
  styleUrl: './victoire.page.scss',
})
export class VictoirePage implements ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly personnageService = inject(PersonnageService);
  private readonly translate = inject(TranslateService);

  readonly personnageId = signal<string | null>(null);
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly nomJoueur = computed(
    () => this.personnage()?.nom ?? this.translate.instant('VICTOIRE.NOM_PAR_DEFAUT'),
  );

  ionViewWillEnter(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.personnageId.set(id);
    if (!id) return;

    this.personnageService.recuperer(id).subscribe({
      next: (p) => this.personnage.set(p),
      error: (err) => console.error(this.translate.instant('VICTOIRE.ERREUR_CHARGEMENT'), err),
    });
  }

  retourAuMenu(): void {
    this.router.navigate(['/accueil']);
  }
}