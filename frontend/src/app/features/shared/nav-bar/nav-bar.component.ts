import { Component, inject, input } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';

/**
 * Barre de navigation basse, commune aux écrans de jeu (chapitre, fiche,
 * journal...). "SAC" n'est pas une route : il ouvre la feuille "SAC À DOS"
 * globale (voir shared/inventaire-sheet/), déjà montée à la racine de
 * l'appli — ce composant se contente de déclencher son ouverture.
 *
 * L'onglet actif est géré par routerLinkActive (Angular), pas par une
 * logique maison : se réactualise automatiquement à chaque navigation, sans
 * input à synchroniser. SAC n'étant pas une route, il ne peut jamais être
 * "actif" au sens navigation, cohérent avec son rôle de simple déclencheur
 * de modale.
 *
 * FICHE et JOURNAL n'ont pas encore d'écran dédié — les liens sont présents
 * mais inertes (aucune route à cibler pour l'instant).
 *
 * IMPORTANT côté intégration : ce composant doit être placé EN DEHORS de
 * tout conteneur avec un padding horizontal (ex. .ecran de chapitre.page),
 * sinon il hérite de ce padding et n'occupe pas toute la largeur de l'écran.
 */
@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './nav-bar.component.html',
  styleUrl: './nav-bar.component.scss',
})
export class NavBarComponent {
  private readonly inventaireSheet = inject(InventaireSheetService);
  private readonly router = inject(Router);

  /** Nécessaire pour construire le lien vers le chapitre et ouvrir le bon sac. */
  readonly personnageId = input.required<string>();

  /**
   * Mort HORS combat (voir Personnage.mort, transmis par la page parente
   * qui a déjà chargé le personnage — pas d'appel HTTP supplémentaire ici).
   * Bloque l'ouverture du sac : les actions qu'il permet (consommer,
   * retirer un objet) seraient de toute façon refusées par le backend.
   */
  readonly mort = input(false);

  ouvrirSac(): void {
    if (this.mort()) return;
    this.inventaireSheet.ouvrir(this.personnageId());
  }

  personnage(): void {
    // Navigation vers la page du personnage
   this.router.navigate(['/personnage', this.personnageId()], { replaceUrl: true });
  }
}