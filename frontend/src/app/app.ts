import {
  Component,
  inject,
  OnInit,
} from '@angular/core';

import { TranslatePipe } from '@ngx-translate/core';

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

import { VersionService } from './core/services/version.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    IonApp,
    IonRouterOutlet,
    InventaireSheetComponent,
    TranslatePipe,
  ],
  template: `
    <ion-app>
      <ion-router-outlet></ion-router-outlet>
      <app-inventaire-sheet></app-inventaire-sheet>
      @if (versionService.miseAJourRequise()) {
        <!-- QUAL-02 : par-dessus toute l'app, la navigation ne peut pas le contourner. -->
        <div class="mise-a-jour" role="alertdialog" aria-labelledby="mise-a-jour-titre">
          <h1 id="mise-a-jour-titre">{{ 'MISE_A_JOUR.TITRE' | translate }}</h1>
          <p>{{ 'MISE_A_JOUR.TEXTE' | translate }}</p>
        </div>
      }
    </ion-app>
  `,
  styles: `
    .mise-a-jour {
      position: fixed;
      inset: 0;
      z-index: 100000;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 24px;
      text-align: center;
      background: var(--ion-background-color, #fff);
      color: var(--ion-text-color, #000);
    }
  `,
})
export class App implements OnInit {

  private readonly router =
    inject(Router);

  protected readonly versionService =
    inject(VersionService);

  ngOnInit(): void {

    if (!Capacitor.isNativePlatform()) {
      return;
    }

    /*
     * QUAL-02 : version minimale, au démarrage puis à chaque
     * retour au premier plan (la version minimale a pu monter).
     */
    void this.versionService.verifier();
    void CapacitorApp.addListener(
      'appStateChange',
      ({ isActive }) => {
        if (isActive) {
          void this.versionService.verifier();
        }
      },
    );

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