import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { DisciplineResume, PersonnageResume } from '../../core/models/personnage.model';
import { DisciplineService } from '../../core/services/discipline.service';
import { PersonnageService } from '../../core/services/personnage.service';
import { NavBarComponent } from '../shared/nav-bar/nav-bar.component';


@Component({
  selector: 'app-personnage',
  standalone: true,
  imports: [IonContent, TranslatePipe, NavBarComponent],
  templateUrl: './personnage.page.html',
  styleUrl: './personnage.page.scss',
})
export class PersonnagePage {
  private readonly route = inject(ActivatedRoute);
  private readonly personnageService = inject(PersonnageService);
  private readonly disciplineService = inject(DisciplineService);

  readonly personnageId = signal<string | null>(this.route.snapshot.paramMap.get('id'));
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  readonly nom = computed(() => this.personnage()?.nom ?? '');
  readonly habilite = computed(() => this.personnage()?.habilite ?? 0);
  readonly habiliteMax = computed(() => this.personnage()?.habiliteBase ?? 0);
  readonly endurance = computed(() => this.personnage()?.enduranceActuelle ?? 0);
  readonly enduranceMax = computed(() => this.personnage()?.enduranceMax ?? 0);
  readonly couronnes = computed(
    () => this.personnage()?.inventaire.find((item) => item.categorie === 'BOURSE')?.quantite ?? 0,
  );
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);
  readonly toutesDisciplines = signal<DisciplineResume[]>([]);
  readonly disciplinesAffichees = computed(() =>
    this.disciplines().map((id) => {
      const discipline = this.toutesDisciplines().find(
        (item) => item.id.toLowerCase() === id.toLowerCase(),
      );
      return discipline ?? { id, nom: id, description: '' };
    }),
  );

  constructor() {
    const id = this.personnageId();
    if (!id) {
      this.erreur.set('PERSONNAGE.ERREUR_ID_MANQUANT');
      this.chargement.set(false);
      return;
    }

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

    this.disciplineService.lister().subscribe({
      next: (disciplines) => this.toutesDisciplines.set(disciplines),
    });
  }
}
