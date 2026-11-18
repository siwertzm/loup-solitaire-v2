import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Location } from '@angular/common';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { Langue, LANGUES, OptionsService, TailleTexte } from '../../core/services/options.service';

/** QUAL-15 : paramètres de l'app (taille du texte, langue ; son à venir). */
@Component({
  selector: 'app-options',
  templateUrl: './options.page.html',
  styleUrls: ['./options.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [IonContent, TranslatePipe],
})
export class OptionsPage {
  private readonly location = inject(Location);
  readonly options = inject(OptionsService);

  readonly tailles: TailleTexte[] = ['PETITE', 'NORMALE', 'GRANDE'];
  readonly langues = LANGUES;

  choisirTaille(taille: TailleTexte): void {
    void this.options.definirTailleTexte(taille);
  }

  choisirLangue(langue: Langue): void {
    void this.options.definirLangue(langue);
  }

  /** Retour à la page d'où l'on vient : accueil ou profil. */
  retour(): void {
    this.location.back();
  }
}
