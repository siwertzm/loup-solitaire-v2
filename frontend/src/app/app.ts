import { Component } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular';
import { InventaireSheetComponent } from './features/shared/inventaire-sheet/inventaire-sheet.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [IonApp, IonRouterOutlet, InventaireSheetComponent],
  template: `
    <ion-app>
      <ion-router-outlet></ion-router-outlet>
      <app-inventaire-sheet></app-inventaire-sheet>
    </ion-app>
  `,
})
export class App {}
