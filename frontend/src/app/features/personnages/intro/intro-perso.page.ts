import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-intro-perso',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './intro-perso.page.html',
  styleUrl: './intro-perso.page.scss',
})
export class IntroPersoPage {
  private readonly router = inject(Router);

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  continuer(): void {
    this.router.navigate(['/personnages/intro']);
  }
}


    