import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
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
  private readonly router = inject(Router);
  readonly options = inject(OptionsService);

  readonly tailles: TailleTexte[] = ['PETITE', 'NORMALE', 'GRANDE'];

  choisirTaille(taille: TailleTexte): void {
    void this.options.definirTailleTexte(taille);
  }

  retour(): void {
    this.router.navigate(['/profil']);
  }
}
