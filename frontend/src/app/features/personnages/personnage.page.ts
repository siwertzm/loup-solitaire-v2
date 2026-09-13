import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, IonIcon, ViewWillEnter } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { informationCircleOutline } from 'ionicons/icons';
import { TranslatePipe } from '@ngx-translate/core';

import { DisciplineResume, PersonnageResume } from '../../core/models/personnage.model';
import { DisciplineService } from '../../core/services/discipline.service';
import { PersonnageService } from '../../core/services/personnage.service';
import { NavBarComponent } from '../shared/nav-bar/nav-bar.component';

addIcons({ 'information-circle-outline': informationCircleOutline });

interface DisciplineAffichee {
  id: string;
  nom: string;
  description: string;
}

@Component({
  selector: 'app-personnage',
  standalone: true,
  imports: [IonContent, IonIcon, TranslatePipe, NavBarComponent],
  templateUrl: './personnage.page.html',
  styleUrl: './personnage.page.scss',
})
export class PersonnagePage implements ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly personnageService = inject(PersonnageService);
  private readonly disciplineService = inject(DisciplineService);

  readonly personnageId = signal<string | null>(this.route.snapshot.paramMap.get('id'));
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly suppressionEnCours = signal(false);
  readonly confirmationSuppression = signal(false);

  readonly nom = computed(() => this.personnage()?.nom ?? '');
  readonly initiale = computed(() => this.nom().trim().charAt(0).toUpperCase() || 'LS');
  readonly chapitre = computed(() => this.personnage()?.chapitreActuelId ?? 1);
  readonly habilite = computed(() => this.personnage()?.habilite ?? 0);
  readonly habiliteMax = computed(() => this.personnage()?.habiliteBase ?? 0);
  readonly endurance = computed(() => this.personnage()?.enduranceActuelle ?? 0);
  readonly enduranceMax = computed(() => this.personnage()?.enduranceMax ?? 0);
  readonly armeMaitrisee = computed(() => this.personnage()?.armeMaitrisee ?? null);
  readonly couronnes = computed(
    () => this.personnage()?.inventaire.find((item) => item.categorie === 'BOURSE')?.quantite ?? 0,
  );
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);
  readonly toutesDisciplines = signal<DisciplineResume[]>([]);
  readonly description = signal<DisciplineAffichee | null>(null);
  readonly disciplinesAffichees = computed<DisciplineAffichee[]>(() =>
    this.disciplines().map((id) => {
      const discipline = this.toutesDisciplines().find(
        (item) => item.id.toLowerCase() === id.toLowerCase(),
      );
      return discipline ?? { id, nom: id, description: '' };
    }),
  );

  ouvrirDescription(discipline: DisciplineAffichee): void {
    this.description.set(discipline);
  }

  fermerDescription(): void {
    this.description.set(null);
  }

  constructor() {
    this.disciplineService.lister().subscribe({
      next: (disciplines) => this.toutesDisciplines.set(disciplines),
    });
  }

  // Ionic garde les pages en cache dans la pile de navigation : sans ce hook,
  // revenir sur cette page après un effet de chapitre (endurance...) réafficherait
  // les anciennes valeurs au lieu de recharger la fiche depuis le backend.
  ionViewWillEnter(): void {
    const id = this.personnageId();
    if (!id) {
      this.erreur.set('PERSONNAGE.ERREUR_ID_MANQUANT');
      this.chargement.set(false);
      return;
    }

    this.chargement.set(true);
    this.erreur.set(null);
    this.personnageService.recuperer(id).subscribe({
      next: (personnage) => {
        this.personnage.set(personnage);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('PERSONNAGE.ERREUR_CHARGEMENT');
        this.chargement.set(false);
      },
    });
  }

  menu(): void {
    this.router.navigate(['/accueil'], { replaceUrl: true });
  }

  ouvrirConfirmationSuppression(): void {
    if (!this.suppressionEnCours()) {
      this.confirmationSuppression.set(true);
    }
  }

  annulerSuppression(): void {
    if (!this.suppressionEnCours()) {
      this.confirmationSuppression.set(false);
    }
  }

  confirmerSuppression(): void {
    const id = this.personnageId();
    if (!id || this.suppressionEnCours()) return;

    this.confirmationSuppression.set(false);
    this.suppressionEnCours.set(true);
    this.erreur.set(null);
    this.personnageService.supprimer(id).subscribe({
      next: () => {
        this.router.navigate(['/accueil'], { replaceUrl: true });
      },
      error: () => {
        this.erreur.set('EQUIPEMENT.ERREUR_SUPPRESSION');
        this.suppressionEnCours.set(false);
      },
    });
  }
}
