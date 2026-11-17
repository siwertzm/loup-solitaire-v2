import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-intro-creation',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './intro-creation.page.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './intro-creation.page.scss',
})
export class IntroCreationPage {
  private readonly router = inject(Router);

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  continuer(): void {
    this.router.navigate(['/personnages/creation']);
  }
}
