import {
  Component,
  inject,
  OnInit,
} from '@angular/core';

import { Router } from '@angular/router';

import {
  IonApp,
  IonRouterOutlet,
} from '@ionic/angular';

import {
  App as CapacitorApp,
} from '@capacitor/app';

import {
  Capacitor,
} from '@capacitor/core';

import {
  InventaireSheetComponent,
} from './features/shared/inventaire-sheet/inventaire-sheet.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    InventaireSheetComponent,
  ],
  template: `
    <ion-app>
      <ion-router-outlet></ion-router-outlet>
      <app-inventaire-sheet></app-inventaire-sheet>
    </ion-app>
  `,
})
export class App implements OnInit {

  private readonly router =
    inject(Router);

  ngOnInit(): void {

    if (!Capacitor.isNativePlatform()) {
      return;
    }

    /*
     * App déjà ouverte ou en arrière-plan.
     */
    void CapacitorApp.addListener(
      'appUrlOpen',
      ({ url }) => {
        this.gererDeepLink(url);
      },
    );

    /*
     * App complètement fermée lorsque
     * l'utilisateur clique sur le lien.
     */
    void CapacitorApp
      .getLaunchUrl()
      .then((resultat) => {

        if (resultat?.url) {
          this.gererDeepLink(
            resultat.url,
          );
        }
      });
  }

  private gererDeepLink(
    url: string,
  ): void {

    if (
      url.startsWith(
        'loupsolitaire://auth/login',
      )
    ) {

      void this.router.navigate(
        ['/auth/login'],
        {
          queryParams: {
            emailVerified: 'true',
          },
        },
      );
    }
  }
}