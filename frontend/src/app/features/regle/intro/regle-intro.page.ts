import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-regle-intro',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './regle-intro.page.html',
  styleUrl: './regle-intro.page.scss',
})
export class RegleIntroPage {
  private readonly router = inject(Router);

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  continuer(): void {
    this.router.navigate(['/regle/disciplines']);
  }
}
