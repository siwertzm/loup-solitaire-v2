import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent, NavController } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Dernière page de la chaîne "Règles du jeu" (intro -> disciplines ->
 * équipement -> combat), voir app.routes.ts. Contenu statique, à l'image
 * de RegleIntroPage : pas de données à charger, uniquement de la
 * documentation sur le fonctionnement réel du combat tel qu'implémenté
 * (CombatPage/CombatService), volontairement différent du système à jets
 * de dés sur papier du livre original (voir backend/README.md).
 */
@Component({
  selector: 'app-regle-combat',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './regle-combat.page.html',
  styleUrl: './regle-combat.page.scss',
})
export class RegleCombatPage {
  private readonly router = inject(Router);
  private readonly navCtrl = inject(NavController);

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  terminer(): void {
    this.router.navigate(['/accueil']);
  }

  back(): void {
    this.navCtrl.navigateBack(['/regle/equipement']);
  }
}
