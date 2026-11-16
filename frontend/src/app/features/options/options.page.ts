import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { OptionsService, TailleTexte } from '../../core/services/options.service';

/** QUAL-15 : paramètres de l'app (taille du texte ; son et langue à venir). */
@Component({
  selector: 'app-options',
  templateUrl: './options.page.html',
  styleUrls: ['./options.page.scss'],
  imports: [IonContent, TranslatePipe],
})
export class OptionsPage {
  private readonly location = inject(Location);
  readonly options = inject(OptionsService);

  readonly tailles: TailleTexte[] = ['PETITE', 'NORMALE', 'GRANDE'];

  choisirTaille(taille: TailleTexte): void {
    void this.options.definirTailleTexte(taille);
  }

  /** Retour à la page d'où l'on vient : accueil ou profil. */
  retour(): void {
    this.location.back();
  }
}
