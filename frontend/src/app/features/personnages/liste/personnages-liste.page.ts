import { Component } from '@angular/core';
import { IonContent, IonHeader, IonTitle, IonToolbar } from '@ionic/angular';

/**
 * Placeholder : listera les personnages du joueur (GET /personnages) et
 * permettra d'en créer un (POST /personnages, exactement 5 disciplines).
 * À implémenter avec PersonnageService une fois les modèles de jeu posés.
 */
@Component({
  selector: 'app-personnages-liste',
  standalone: true,
  imports: [IonContent, IonHeader, IonTitle, IonToolbar],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Mes personnages</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <p>À venir : liste des personnages + création.</p>
    </ion-content>
  `,
})
export class PersonnagesListePage {}
