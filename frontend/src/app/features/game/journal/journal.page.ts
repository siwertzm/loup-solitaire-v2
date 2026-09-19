import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { PersonnageService } from '../../../core/services/personnage.service';
import { NavBarComponent } from '../../shared/nav-bar/nav-bar.component';

/**
 * Journal du parcours : les chapitres traversés par le personnage, dans
 * l'ordre chronologique — le premier chapitre en haut, la liste descend au
 * fil de l'histoire, le chapitre courant est tout en bas.
 *
 * Chaque ARRIVÉE compte : un chapitre revisité (retour payant après une mort
 * narrative, retour après une défaite) apparaît plusieurs fois, d'où le
 * `track $index` du template — les numéros ne sont pas uniques.
 *
 * Deux appels en parallèle : l'historique, et la fiche du personnage — celle-ci
 * uniquement pour transmettre `mort` à la barre de navigation (elle bloque le
 * sac quand le personnage est mort, voir NavBarComponent).
 *
 * Rechargé à chaque entrée sur la page (ionViewWillEnter) : Ionic garde les
 * pages en cache, et le journal a pu changer depuis la dernière visite.
 */
@Component({
  selector: 'app-journal',
  standalone: true,
  imports: [IonContent, TranslatePipe, NavBarComponent],
  templateUrl: './journal.page.html',
  styleUrl: './journal.page.scss',
})
export class JournalPage implements ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly personnageService = inject(PersonnageService);

  readonly personnageId = signal<string | null>(this.route.snapshot.paramMap.get('id'));
  readonly chapitres = signal<number[]>([]);
  readonly mort = signal(false);
  readonly chargement = signal(true);
  /** Clé de traduction du message d'erreur (comme sur les autres pages). */
  readonly erreur = signal<string | null>(null);

  readonly nombre = computed(() => this.chapitres().length);

  ionViewWillEnter(): void {
    const id = this.personnageId();
    if (!id) {
      this.erreur.set('JOURNAL.ERREUR_ID_MANQUANT');
      this.chargement.set(false);
      return;
    }

    this.chargement.set(true);
    this.erreur.set(null);
    forkJoin({
      personnage: this.personnageService.recuperer(id),
      chapitres: this.personnageService.historique(id),
    }).subscribe({
      next: ({ personnage, chapitres }) => {
        this.mort.set(personnage.mort);
        // L'API renvoie du plus récent au plus ancien : on retourne la liste
        // pour afficher le parcours dans l'ordre où il a été vécu.
        this.chapitres.set(chapitres);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('JOURNAL.ERREUR_CHARGEMENT');
        this.chargement.set(false);
      },
    });
  }
}