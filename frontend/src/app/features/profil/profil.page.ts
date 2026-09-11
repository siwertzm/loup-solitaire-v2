import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';

import { MoiResponse } from '../../core/models/personnage.model';
import { PersonnageService } from '../../core/services/personnage.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [IonContent],
  templateUrl: './profil.page.html',
  styleUrl: './profil.page.scss',
})
export class ProfilPage implements ViewWillEnter {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);
  private readonly auth = inject(AuthService);

  readonly compte = signal<MoiResponse | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly initiale = computed(() => (this.compte()?.username ?? '?').charAt(0).toUpperCase());

  readonly lignes = computed(() => {
    const c = this.compte();
    if (!c) return [];
    return [
      { cle: 'PERSONNAGES', valeur: String(c.personnages?.length ?? 0), ton: 'clair' },
      { cle: 'EMAIL VÉRIFIÉ', valeur: c.emailVerifie ? 'oui' : 'non', ton: c.emailVerifie ? 'vert' : 'rouge' },
      { cle: 'PIÈCES PREMIUM', valeur: "999", ton: 'gold' },
      { cle: 'MEMBRE DEPUIS', valeur: "mars 2026", ton: 'clair' }
    ];
  });

  // ionViewWillEnter (et non ngOnInit) : ion-router-outlet garde cette page en
  // mémoire quand on va sur /profil/edition, le composant n'est pas recréé au
  // retour. Ce hook Ionic se redéclenche bien à chaque fois que la page
  // redevient active, contrairement à ngOnInit qui ne tourne qu'une fois.
  ionViewWillEnter(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.personnages$.moi().subscribe({
      next: (c) => {
        this.compte.set(c);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger ton profil.');
        this.chargement.set(false);
      },
    });
  }

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  modifierEmail(): void {
    this.router.navigate(['/profil/edition']);
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login'], { replaceUrl: true }),
      // Le refresh token était déjà invalide côté serveur : on nettoie quand
      // même localement pour ne pas bloquer l'utilisateur sur cet écran.
      error: () => {
        this.auth.clearSessionLocale();
        this.router.navigate(['/auth/login'], { replaceUrl: true });
      },
    });
  }
}