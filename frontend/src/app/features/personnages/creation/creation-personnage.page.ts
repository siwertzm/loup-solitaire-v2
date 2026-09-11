import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { IdDiscipline, NB_DISCIPLINES_A_CHOISIR } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { DisciplineService } from '../../../core/services/discipline.service';

@Component({
  selector: 'app-creation-personnage',
  standalone: true,
  imports: [IonContent, ReactiveFormsModule],
  templateUrl: './creation-personnage.page.html',
  styleUrl: './creation-personnage.page.scss',
})
export class CreationPersonnagePage {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);
  private readonly disciplines$ = inject(DisciplineService);

  readonly nom = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  /** null tant que le joueur n'a pas lancé le dé. */
  readonly habilete = signal<number | null>(null);
  readonly endurance = signal<number | null>(null);
  readonly face = signal<number | string>('?');
  readonly roulant = signal(false);

  readonly choisies = signal<IdDiscipline[]>([]);
  readonly erreur = signal<string | null>(null);
  readonly envoi = signal(false);

  /** Catalogue des 10 Disciplines Kaï (id, nom, description), depuis GET /disciplines. */
  readonly catalogue = signal<{ id: IdDiscipline; nom: string; resume: string }[]>([]);
  readonly chargement = signal(true);

  readonly tirageFait = computed(() => this.habilete() !== null);
  readonly tirageLabel = computed(() => (this.tirageFait() ? 'TIRAGE DES CARACTÉRISTIQUES' : 'LANCE LE DÉ'));
  readonly habileteAffichee = computed(() => this.habilete() ?? '—');
  readonly enduranceAffichee = computed(() => this.endurance() ?? '—');
  readonly nbChoisies = computed(() => this.choisies().length);
  readonly complet = computed(
    () => this.nom.value.trim().length > 0 && this.tirageFait() && this.nbChoisies() === NB_DISCIPLINES_A_CHOISIR,
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
        this.erreur.set('Impossible de charger les disciplines.');
        this.chargement.set(false);
      },
    });
  }

  /**
   * Tirage côté client : purement cosmétique.
   * Le backend refait les tirages dans `PersonnageService.creerPersonnage`,
   * les valeurs affichées ici sont donc remplacées par sa réponse.
   */
  lancerDe(): void {
    if (this.roulant()) return;
    this.roulant.set(true);
    this.erreur.set(null);

    let tours = 0;
    const spin = setInterval(() => {
      tours++;
      this.face.set(Math.floor(Math.random() * 10));
      if (tours > 8) {
        clearInterval(spin);
        const dHab = Math.floor(Math.random() * 10);
        const dEnd = Math.floor(Math.random() * 10);
        this.face.set(dHab);
        this.habilete.set(10 + dHab);
        this.endurance.set(20 + dEnd);
        this.roulant.set(false);
      }
    }, 70);
  }

  basculer(id: IdDiscipline): void {
    const prises = this.choisies();
    if (prises.includes(id)) {
      this.choisies.set(prises.filter((x) => x !== id));
      this.erreur.set(null);
      return;
    }
    if (prises.length >= NB_DISCIPLINES_A_CHOISIR) {
      this.erreur.set(`Exactement ${NB_DISCIPLINES_A_CHOISIR} disciplines : retires-en une d'abord.`);
      return;
    }
    this.choisies.set([...prises, id]);
    this.erreur.set(null);
  }

  creer(): void {
    if (this.envoi()) return;
    if (!this.nom.value.trim()) return this.erreur.set('Donne un nom à ton personnage.');
    if (!this.tirageFait()) return this.erreur.set('Lance le dé pour tirer tes caractéristiques.');
    if (this.nbChoisies() !== NB_DISCIPLINES_A_CHOISIR) {
      return this.erreur.set(`Choisis exactement ${NB_DISCIPLINES_A_CHOISIR} disciplines.`);
    }

    this.envoi.set(true);
    this.erreur.set(null);
    const hasardHabilite = this.habilete()! - 10;
    const hasardEndurance = this.endurance()! - 20;
    this.personnages$.creer(this.nom.value.trim(), this.choisies(), hasardHabilite, hasardEndurance).subscribe({
      next: (p) => {
        this.envoi.set(false);
        this.router.navigate(['/personnages', p.id, 'chapitre'], { replaceUrl: true });
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('La création a échoué.');
      },
    });
  }

  retour(): void {
    this.router.navigate(['/accueil']);
  }
}