import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { ChapitreParcouru } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { NavBarComponent } from '../../shared/nav-bar/nav-bar.component';

/**
 * Journal du parcours : les chapitres traversés par le personnage, du plus
 * récent au plus ancien — le dernier chapitre lu est en haut (c'est l'ordre
 * renvoyé par l'API, affiché tel quel).
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
 * pages en cache, et le journal a pu changer depuis la dernière visite. Pour la
 * même raison, la page est aussi remise en haut à chaque entrée : sinon on
 * retrouverait la position de défilement de la visite précédente.
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
  readonly chapitres = signal<ChapitreParcouru[]>([]);
  readonly mort = signal(false);
  readonly chargement = signal(true);
  /** Clé de traduction du message d'erreur (comme sur les autres pages). */
  readonly erreur = signal<string | null>(null);

  readonly nombre = computed(() => this.chapitres().length);

  /** Le conteneur défilant, pour remettre la page en haut à chaque entrée. */
  private readonly contenu = viewChild(IonContent);

  ionViewWillEnter(): void {
    // Retour en haut à chaque entrée (durée 0 : instantané, avant l'affichage).
    // Sur la toute première entrée, la référence peut ne pas encore exister :
    // sans conséquence, la page est déjà en haut.
    void this.contenu()?.scrollToTop(0);

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
        // L'API renvoie du plus récent au plus ancien : on l'affiche tel quel,
        // le dernier chapitre lu est en haut.
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