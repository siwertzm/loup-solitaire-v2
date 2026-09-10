import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { PersonnageResume } from '../../core/models/personnage.model';
import { PersonnageService } from '../../core/services/personnage.service';

@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [IonContent],
  templateUrl: './accueil.page.html',
  styleUrl: './accueil.page.scss',
})
export class AccueilPage {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);

  readonly personnages = signal<PersonnageResume[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  /** Le premier personnage de la liste est celui que l'on « reprend ». */
  readonly actifId = signal<string | null>(null);

  readonly cartes = computed(() =>
    this.personnages().map((p) => ({
      ...p,
      actif: p.id === this.actifId(),
      habileteTotale: p.habilite + (p.habiliteTemp ?? 0),
      initiale: p.nom.charAt(0).toUpperCase(),
    })),
  );

  /** Masques de fondu haut/bas du carrousel. */
  readonly scrolle = signal(false);
  readonly enFin = signal(false);
  readonly masque = computed(() => {
    if (!this.scrolle() && this.enFin()) return 'none';
    const haut = this.scrolle() ? 'transparent 0, rgba(0,0,0,.35) 12px, #000 34px' : '#000 0';
    const bas = this.enFin()
      ? '#000 100%'
      : '#000 calc(100% - 34px), rgba(0,0,0,.35) calc(100% - 12px), transparent 100%';
    return `linear-gradient(to bottom, ${haut}, ${bas})`;
  });

  ngOnInit(): void {
    this.personnages$.lister().subscribe({
      next: (liste) => {
        this.personnages.set(liste);
        this.actifId.set(liste[0]?.id ?? null);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('Impossible de charger tes personnages.');
        this.chargement.set(false);
      },
    });
  }

  onScroll(event: Event): void {
    const el = event.currentTarget as HTMLElement;
    this.scrolle.set(el.scrollTop > 6);
    this.enFin.set(el.scrollTop + el.clientHeight >= el.scrollHeight - 6);
  }

  jouer(p: PersonnageResume): void {
    this.actifId.set(p.id);
    this.router.navigate(['/personnages', p.id, 'chapitre']);
  }

  nouveauPersonnage(): void {
    this.router.navigate(['/personnages/creation']);
  }

  regles(): void {
    this.router.navigate(['/regles']);
  }

  profil(): void {
    this.router.navigate(['/profil']);
  }
}