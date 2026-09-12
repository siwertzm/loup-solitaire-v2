import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { DisciplineResume } from '../../../core/models/personnage.model';
import { DisciplineService } from '../../../core/services/discipline.service';


@Component({
  selector: 'app-regle-discipline',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './regle-discipline.page.html',
  styleUrl: './regle-discipline.page.scss',
})
export class RegleDisciplinePage {
  private readonly router = inject(Router);
  private readonly disciplines$ = inject(DisciplineService);

  readonly disciplines = signal<DisciplineResume[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.disciplines$.lister().subscribe({
      next: (disciplines) => {
        this.disciplines.set(disciplines);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger les disciplines.');
        this.chargement.set(false);
      },
    });
  }

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  continuer(): void {
    this.router.navigate(['/regle/equipement']);
  }
}
