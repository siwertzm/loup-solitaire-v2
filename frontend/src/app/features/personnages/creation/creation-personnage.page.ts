import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, forkJoin, shareReplay, timer } from 'rxjs';

import { IdDiscipline, NB_DISCIPLINES_A_CHOISIR, TirageCreation } from '../../../core/models/personnage.model';
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
  readonly habilite = signal<number | null>(null);
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

  /**
   * Tirage fait par le serveur (POST /personnages/tirage), demandé une seule
   * fois pour les deux dés puis partagé : le serveur renvoie de toute façon
   * le même tirage tant que le personnage n'est pas créé.
   */
  private tirage$?: Observable<TirageCreation>;

  /** Catalogue des 10 Disciplines Kaï (id, nom, description), depuis GET /disciplines. */
  readonly catalogue = signal<DisciplineCatalogue[]>([]);
  readonly chargement = signal(true);

  readonly tirageFait = computed(() => this.habilite() !== null && this.endurance() !== null);
  readonly tirageLabel = computed(() => (this.tirageFait() ? 'CREATION_PERSONNAGE.LABEL_TIRAGE_FAIT' : 'CREATION_PERSONNAGE.LABEL_TIRAGE_EN_ATTENTE'));
  readonly habTiree = computed(() => this.habilite() !== null && !this.roulantHab());
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
   * Lance un dé : le résultat vient du serveur (SEC-01), l'animation dure au
   * moins 640 ms. Un dé déjà tiré ne se relance pas.
   */
  lancerDe(quoi: 'hab' | 'end'): void {
    const roulant = quoi === 'hab' ? this.roulantHab : this.roulantEnd;
    const dejaTire = quoi === 'hab' ? this.habilite() !== null : this.endurance() !== null;
    if (dejaTire || roulant()) return;
    this.erreur.set(null);
    roulant.set(true);

    forkJoin([this.tirage(), timer(640)]).subscribe({
      next: ([tirage]) => {
        if (quoi === 'hab') {
          this.faceHab.set(tirage.hasardHabilite);
          this.habilite.set(tirage.habilite);
        } else {
          this.faceEnd.set(tirage.hasardEndurance);
          this.endurance.set(tirage.endurance);
        }
        roulant.set(false);
      },
      error: () => {
        // Permet de réessayer : la prochaine demande repart vers le serveur.
        this.tirage$ = undefined;
        roulant.set(false);
        this.erreur.set('CREATION_PERSONNAGE.ERREUR_TIRAGE');
      },
    });
  }

  private tirage(): Observable<TirageCreation> {
    this.tirage$ ??= this.personnages$.tirer().pipe(shareReplay(1));
    return this.tirage$;
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
    this.personnages$.creer(nomFinal, this.choisies()).subscribe({
      next: (p) => {
        this.envoi.set(false);
        this.router.navigate(['/personnages', p.id, 'inventaire', 'intro'], { replaceUrl: true });
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