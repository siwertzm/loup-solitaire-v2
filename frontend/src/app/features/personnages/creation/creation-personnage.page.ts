import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { IdDiscipline, NB_DISCIPLINES_A_CHOISIR } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { DisciplineService } from '../../../core/services/discipline.service';

interface DisciplineCatalogue {
  id: IdDiscipline;
  nom: string;
  resume: string;
}

@Component({
  selector: 'app-creation-personnage',
  standalone: true,
  imports: [IonContent, ReactiveFormsModule, TranslatePipe],
  templateUrl: './creation-personnage.page.html',
  styleUrl: './creation-personnage.page.scss',
})
export class CreationPersonnagePage {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);
  private readonly disciplines$ = inject(DisciplineService);
  private readonly translate = inject(TranslateService);

  readonly nom = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  /** null tant que le dé correspondant n'a pas été lancé. */
  readonly habilete = signal<number | null>(null);
  readonly endurance = signal<number | null>(null);
  readonly faceHab = signal<number | string>('?');
  readonly faceEnd = signal<number | string>('?');
  readonly roulantHab = signal(false);
  readonly roulantEnd = signal(false);

  readonly choisies = signal<IdDiscipline[]>([]);
  readonly erreur = signal<string | null>(null);
  readonly erreurParams = signal<Record<string, unknown> | undefined>(undefined);
  readonly envoi = signal(false);
  readonly description = signal<DisciplineCatalogue | null>(null);

  /** Catalogue des 10 Disciplines Kaï (id, nom, description), depuis GET /disciplines. */
  readonly catalogue = signal<DisciplineCatalogue[]>([]);
  readonly chargement = signal(true);

  readonly tirageFait = computed(() => this.habilete() !== null && this.endurance() !== null);
  readonly tirageLabel = computed(() => (this.tirageFait() ? 'CREATION_PERSONNAGE.LABEL_TIRAGE_FAIT' : 'CREATION_PERSONNAGE.LABEL_TIRAGE_EN_ATTENTE'));
  readonly habTiree = computed(() => this.habilete() !== null && !this.roulantHab());
  readonly endTiree = computed(() => this.endurance() !== null && !this.roulantEnd());
  readonly detailHab = computed(() => `10 + ${this.faceHab()}`);
  readonly detailEnd = computed(() => `20 + ${this.faceEnd()}`);
  readonly nbChoisies = computed(() => this.choisies().length);
  readonly complet = computed(
    () => this.tirageFait() && this.nbChoisies() === NB_DISCIPLINES_A_CHOISIR,
  );

  readonly disciplines = computed(() => {
    const prises = this.choisies();
    return this.catalogue().map((d) => ({
      ...d,
      prise: prises.includes(d.id),
      bloquee: !prises.includes(d.id) && prises.length >= NB_DISCIPLINES_A_CHOISIR,
    }));
  });

  ngOnInit(): void {
    this.disciplines$.lister().subscribe({
      next: (liste) => {
        this.catalogue.set(liste.map((d) => ({ id: d.id, nom: d.nom, resume: d.description })));
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('CREATION_PERSONNAGE.ERREUR_CHARGEMENT_DISCIPLINES');
        this.chargement.set(false);
      },
    });
  }

  /**
   * Tirage côté client : purement cosmétique (le backend refait les tirages
   * dans PersonnageService.creerPersonnage à partir des valeurs qu'on lui
   * envoie). Une seule écriture d'état par lancer : l'animation du dé est en CSS.
   */
  lancerDe(quoi: 'hab' | 'end'): void {
    if (quoi === 'hab' ? this.roulantHab() : this.roulantEnd()) return;
    this.erreur.set(null);
    const de = Math.floor(Math.random() * 10);

    if (quoi === 'hab') {
      this.roulantHab.set(true);
      setTimeout(() => {
        this.faceHab.set(de);
        this.habilete.set(10 + de);
        this.roulantHab.set(false);
      }, 640);
    } else {
      this.roulantEnd.set(true);
      setTimeout(() => {
        this.faceEnd.set(de);
        this.endurance.set(20 + de);
        this.roulantEnd.set(false);
      }, 640);
    }
  }

  ouvrirDescription(d: DisciplineCatalogue): void {
    this.description.set(d);
  }

  fermerDescription(): void {
    this.description.set(null);
  }

  basculer(id: IdDiscipline): void {
    const prises = this.choisies();
    if (prises.includes(id)) {
      this.choisies.set(prises.filter((x) => x !== id));
      this.erreur.set(null);
      return;
    }
    if (prises.length >= NB_DISCIPLINES_A_CHOISIR) {
      this.erreur.set('CREATION_PERSONNAGE.ERREUR_TROP_DE_DISCIPLINES');
      this.erreurParams.set({ n: NB_DISCIPLINES_A_CHOISIR });
      return;
    }
    this.choisies.set([...prises, id]);
    this.erreur.set(null);
  }

  creer(): void {
    if (this.envoi()) return;
    if (!this.tirageFait()) return this.erreur.set('CREATION_PERSONNAGE.ERREUR_TIRAGE_MANQUANT');
    if (this.nbChoisies() !== NB_DISCIPLINES_A_CHOISIR) {
      this.erreur.set('CREATION_PERSONNAGE.ERREUR_NOMBRE_DISCIPLINES');
      this.erreurParams.set({ n: NB_DISCIPLINES_A_CHOISIR });
      return;
    }

    this.envoi.set(true);
    this.erreur.set(null);
    const nomFinal = this.nom.value.trim() || this.translate.instant('CREATION_PERSONNAGE.NOM');
    const hasardHabilite = this.habilete()! - 10;
    const hasardEndurance = this.endurance()! - 20;
    this.personnages$.creer(nomFinal, this.choisies(), hasardHabilite, hasardEndurance).subscribe({
      next: (p) => {
        this.envoi.set(false);
        this.router.navigate(['/personnages', p.id, 'chapitre'], { replaceUrl: true });
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('CREATION_PERSONNAGE.ERREUR_CREATION');
      },
    });
  }

  retour(): void {
    this.router.navigate(['/accueil']);
  }
}