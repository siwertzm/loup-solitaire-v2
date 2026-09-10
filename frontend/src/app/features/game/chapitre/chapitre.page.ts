import { Component } from '@angular/core';
import { IonContent, IonHeader, IonTitle, IonToolbar } from '@ionic/angular';

/**
 * Placeholder : écran central du jeu (GET /personnages/{id}/chapitre).
 * À implémenter avec ChapitreService : texte, liens filtrés, objets proposés,
 * redirection vers l'écran de combat si combat = true.
 */
@Component({
  selector: 'app-chapitre',
  standalone: true,
  imports: [IonContent, IonHeader, IonTitle, IonToolbar],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Chapitre</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <p>À venir : texte du chapitre, liens, objets, combat.</p>
    </ion-content>
  `,
})
export class ChapitrePage {}
