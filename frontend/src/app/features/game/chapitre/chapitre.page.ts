import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { PersonnageResume } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { ChapitreResponse, LienResponse } from '../../../core/models/chapitre.model';
import { ChapitreService } from '../../../core/services/chapitre.service';

/**
 * Écran central du jeu : affiche le chapitre en cours et la fiche du personnage.
 */
@Component({
  selector: 'app-chapitre',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './chapitre.page.html',
  styleUrl: './chapitre.page.scss',
})
export class ChapitrePage implements OnInit, ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chapitreService = inject(ChapitreService);
  private readonly personnageService = inject(PersonnageService);

  readonly personnageId = signal<string | null>(null);
  
  // Signal stockant les infos du personnage
  readonly personnage = signal<PersonnageResume | null>(null);
  
  // Signal stockant le chapitre courant
  readonly chapitre = signal<ChapitreResponse | null>(null);
  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);

  // Computed properties pour accéder facilement aux infos du personnage
  readonly nomPersonnage = computed(() => this.personnage()?.nom ?? '');
  readonly initiale = computed(() => {
    const nom = this.personnage()?.nom?.trim();
    if (!nom) return 'LS';
    return nom
      .split(/\s+/)
      .map(mot => mot.charAt(0).toUpperCase())
      .join('');
  });
  readonly habilite = computed(() => (this.personnage()?.habilite ?? 0) + (this.personnage()?.habiliteTemp ?? 0));
  readonly habiliteTemp = computed(() => this.personnage()?.habiliteTemp ?? 0);
  readonly enduranceActuelle = computed(() => this.personnage()?.enduranceActuelle ?? 0);

  // Pourcentages pour le remplissage visuel des barres
  readonly endurancePourcentage = computed(() => {
    const max = this.enduranceMax();
    return Math.min(100, Math.max(0, (this.enduranceActuelle() / max) * 100));
  });

  readonly habilitePourcentage = computed(() => {
    const max = this.habilite(); // Valeur d'Habileté maximale estimée
    return Math.min(100, Math.max(0, ((this.habilite() + this.habiliteTemp()) / max) * 100));
  });

  readonly enduranceMax = computed(() => this.personnage()?.enduranceMax ?? 0);
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);
  readonly inventaire = computed(() => this.personnage()?.inventaire ?? []);

  // Computed properties du chapitre
  readonly liens = computed(() => this.chapitre()?.liens ?? []);
  readonly objets = computed(() => this.chapitre()?.objets ?? []);
  readonly ennemis = computed(() => this.chapitre()?.ennemis ?? []);
  readonly effets = computed(() => this.chapitre()?.effets ?? []);
  readonly estCombat = computed(() => this.chapitre()?.combat ?? false);

  ngOnInit(): void {
    this.initialiserId();
  }

  ionViewWillEnter(): void {
    this.initialiserId();
    if (this.personnageId()) {
      this.chargerToutesLesDonnees();
    }
  }

  private initialiserId(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.personnageId.set(id);
    } else {
      this.erreur.set('Identifiant de personnage introuvable.');
      this.chargement.set(false);
    }
  }

  /**
   * Récupère à la fois les informations du personnage et du chapitre courant.
   */
  chargerToutesLesDonnees(): void {
    const id = this.personnageId();
    if (!id) return;

    this.chargement.set(true);
    this.erreur.set(null);

    // 1. Récupération du personnage (GET /personnages/{id})
    this.personnageService.recuperer(id).subscribe({
      next: (p) => {
        this.personnage.set(p);
      },
      error: (err) => console.error('Erreur lors du chargement du personnage :', err)
    });

    // 2. Récupération du chapitre (GET /personnages/{id}/chapitre)
    this.chapitreService.getChapitreCourant(id).subscribe({
      next: (data) => {
        this.chapitre.set(data);
        this.chargement.set(false);
      },
      error: (err) => {
        console.error('Erreur lors de la récupération du chapitre :', err);
        this.erreur.set('Impossible de charger le chapitre en cours.');
        this.chargement.set(false);
      },
    });
  }

  /**
   * Avance vers un chapitre cible choisi par le joueur.
   */
  choisirLien(lien: LienResponse): void {
    const id = this.personnageId();
    if (!id || !lien.disponible) return;

    this.chargement.set(true);
    this.chapitreService.avancerVersChapitre(id, lien.chapitreCibleId).subscribe({
      next: () => {
        // Recharge les données mises à jour du personnage et du nouveau chapitre
        this.chargerToutesLesDonnees();
      },
      error: (err) => {
        console.error('Erreur lors de l\'avancement vers le chapitre :', err);
        this.erreur.set('Impossible d\'avancer vers ce chapitre.');
        this.chargement.set(false);
      },
    });
  }

  /** Redirige vers la page d'accueil des personnages. */
  retourAccueil(): void {
    this.router.navigate(['/accueil']);
  }
}