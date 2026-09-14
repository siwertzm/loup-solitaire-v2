import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent, IonIcon, ViewWillEnter } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { personCircleOutline } from 'ionicons/icons';
import { TranslatePipe } from '@ngx-translate/core';

import { PersonnageResume } from '../../core/models/personnage.model';
import { PersonnageService } from '../../core/services/personnage.service';

addIcons({ 'person-circle-outline': personCircleOutline });

@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [IonContent, IonIcon, TranslatePipe],
  templateUrl: './accueil.page.html',
  styleUrl: './accueil.page.scss',
})
export class AccueilPage implements ViewWillEnter {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);

  readonly personnages = signal<PersonnageResume[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  /** Le premier personnage de la liste est celui que l'on « reprend ». */
  readonly actifId = signal<string | null>(null);

  // Suppression depuis la carte d'un personnage mort : confirmation avant
  // action destructive, comme sur PersonnagePage. On stocke l'id concerné
  // (pas juste un booléen) car plusieurs cartes peuvent être mortes en même
  // temps dans le carrousel.
  readonly confirmationSuppressionId = signal<string | null>(null);
  readonly suppressionEnCoursId = signal<string | null>(null);
  readonly erreurSuppression = signal<string | null>(null);
  readonly nomEnConfirmation = computed(
    () => this.personnages().find((p) => p.id === this.confirmationSuppressionId())?.nom ?? '',
  );

  readonly cartes = computed(() =>
    this.personnages().map((p) => ({
      ...p,
      actif: p.id === this.actifId(),
      habileteTotale: p.habilite + (p.habiliteTemp ?? 0),
      initiale: p.nom.charAt(0).toUpperCase() || 'LS',
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

  // ionViewWillEnter : se redéclenche à chaque retour sur cette page (ex. après
  // avoir joué un chapitre), contrairement à ngOnInit qui ne tourne qu'une fois
  // tant qu'ion-router-outlet garde le composant en mémoire.
  ionViewWillEnter(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.personnages$.lister().subscribe({
      next: (liste) => {
        this.personnages.set(liste);
        this.actifId.set(liste[0]?.id ?? null);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('ACCUEIL.ERREUR_CHARGEMENT');
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

  /**
   * Personnage mort : renvoie vers ChapitrePage plutôt que de dupliquer ici
   * la logique de résurrection (POST /ressusciter + cas du lien de retour
   * narratif) déjà centralisée là-bas.
   */
  ressusciter(p: PersonnageResume): void {
    this.jouer(p);
  }

  ouvrirConfirmationSuppression(personnageId: string): void {
    if (!this.suppressionEnCoursId()) {
      this.erreurSuppression.set(null);
      this.confirmationSuppressionId.set(personnageId);
    }
  }

  annulerSuppression(): void {
    if (!this.suppressionEnCoursId()) {
      this.confirmationSuppressionId.set(null);
      this.erreurSuppression.set(null);
    }
  }

  confirmerSuppression(): void {
    const id = this.confirmationSuppressionId();
    if (!id || this.suppressionEnCoursId()) return;

    this.suppressionEnCoursId.set(id);
    this.erreurSuppression.set(null);
    this.personnages$.supprimer(id).subscribe({
      next: () => {
        this.personnages.update((liste) => liste.filter((p) => p.id !== id));
        if (this.actifId() === id) {
          this.actifId.set(this.personnages()[0]?.id ?? null);
        }
        this.suppressionEnCoursId.set(null);
        this.confirmationSuppressionId.set(null);
      },
      // Popup laissée ouverte (pas de confirmationSuppressionId.set(null))
      // pour permettre de réessayer sans rouvrir la confirmation.
      error: () => {
        this.erreurSuppression.set('EQUIPEMENT.ERREUR_SUPPRESSION');
        this.suppressionEnCoursId.set(null);
      },
    });
  }

  nouveauPersonnage(): void {
    this.router.navigate(['/personnages/intro']);
  }

  regles(): void {
    this.router.navigate(['/regle/intro']);
  }

  profil(): void {
    this.router.navigate(['/profil']);
  }
}