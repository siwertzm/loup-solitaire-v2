import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
/**
 * Placeholder : écran central du jeu (GET /personnages/{id}/chapitre).
 * À implémenter avec ChapitreService : texte, liens filtrés, objets proposés,
 * redirection vers l'écran de combat si combat = true.
 */
@Component({
  selector: 'app-chapitre',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './chapitre.page.html',
  styleUrl: './chapitre.page.scss',
})
export class ChapitrePage {
  private router = inject(Router);
}
