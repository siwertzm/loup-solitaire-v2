import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';

import { PersonnageResume } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { ChapitreResponse, LienResponse } from '../../../core/models/chapitre.model';
import { ChapitreService } from '../../../core/services/chapitre.service';
import { DisciplineResume } from '../../../core/models/personnage.model';
import { DisciplineService } from '../../../core/services/discipline.service';
import { ObjetResume } from '../../../core/models/personnage.model';
import { ObjetService } from '../../../core/services/objet.service';
import { InventaireSheetService } from '../../../core/services/inventaire-sheet.service';
import { ChapitreObjetsComponent } from './objets/chapitre-objets.component';
import { ChapitreEffetsComponent } from './effets/chapitre-effets.component';
import { NavBarComponent } from '../../shared/nav-bar/nav-bar.component';

/**
 * Écran central du jeu : affiche le chapitre en cours et la fiche du personnage.
 *
 * La liste des objets du chapitre + le résumé d'inventaire ("sac") sont
 * délégués à <app-chapitre-objets> (dossier objets/) pour garder ce fichier
 * gérable — ce composant reste seul propriétaire des données (personnage,
 * chapitre, catalogues disciplines/objets) et de la navigation.
 */
@Component({
  selector: 'app-chapitre',
  standalone: true,
  imports: [IonContent, RouterLink, ChapitreObjetsComponent, ChapitreEffetsComponent, NavBarComponent],
  templateUrl: './chapitre.page.html',
  styleUrl: './chapitre.page.scss',
})
export class ChapitrePage implements OnInit, ViewWillEnter {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chapitreService = inject(ChapitreService);
  private readonly personnageService = inject(PersonnageService);
  private readonly disciplineService = inject(DisciplineService);
  private readonly objetService = inject(ObjetService);
  private readonly inventaireSheet = inject(InventaireSheetService);

  readonly personnageId = signal<string | null>(null);

  // Signal stockant les infos du personnage
  readonly personnage = signal<PersonnageResume | null>(null);
  readonly initiale = computed(() => this.nomPersonnage().trim().charAt(0).toUpperCase() || 'LS');

  // Signal stockant tous les objets disponibles (catalogue, pour nomObjet()
  // et transmis à <app-chapitre-objets> pour la résolution des icônes)
  readonly tousObjets = signal<ObjetResume[]>([]);

  // Signal stockant le chapitre courant
  readonly chapitre = signal<ChapitreResponse | null>(null);
  readonly chargement = signal<boolean>(true);
  readonly erreur = signal<string | null>(null);

  // Signal stockant toutes les disciplines disponibles
  readonly toutesDisciplines = signal<DisciplineResume[]>([]);

  // Computed properties pour accéder facilement aux infos du personnage
  readonly nomPersonnage = computed(() => this.personnage()?.nom ?? '');
  
  readonly habilite = computed(
    () => (this.personnage()?.habilite ?? 0) + (this.personnage()?.habiliteTemp ?? 0),
  );
  readonly enduranceActuelle = computed(() => this.personnage()?.enduranceActuelle ?? 0);

  // Pourcentages pour le remplissage visuel des barres
  readonly endurancePourcentage = computed(() => {
    const max = this.enduranceMax();
    return Math.min(100, Math.max(0, (this.enduranceActuelle() / max) * 100));
  });

  readonly habilitePourcentage = computed(() => {
    const max = this.habilite(); // Valeur d'Habileté maximale estimée
    return Math.min(100, Math.max(0, (this.habilite() / max) * 100));
  });

  readonly enduranceMax = computed(() => this.personnage()?.enduranceMax ?? 0);
  readonly disciplines = computed(() => this.personnage()?.disciplines ?? []);

  // Computed properties du chapitre
  readonly liens = computed(() => this.chapitre()?.liens ?? []);
  readonly objets = computed(() => this.chapitre()?.objets ?? []);
  readonly ennemis = computed(() => this.chapitre()?.ennemis ?? []);
  readonly effets = computed(() => this.chapitre()?.effets ?? []);
  readonly estCombat = computed(() => this.chapitre()?.combat ?? false);

  readonly ongletActif = signal<'chapitre' | 'combat' | 'objets' | 'effets'>('chapitre');
  readonly ongletLeve = signal<'chapitre' | 'combat' | 'objets' | 'effets' | null>(null);
  readonly ongletObjetsClique = signal(false);
  private readonly chapitreObjetCliqueKey = 'loup-solitaire:chapitre-objet-clique';
  private readonly chapitreHasardTermineKey = 'loup-solitaire:chapitre-hasard-termine';

  // Signaux pour gérer le hasard (révélation et roulement)
  readonly hasardRoule = signal(false);
  readonly hasardResultatVisible = signal(false);
  readonly hasardTermine = signal(false);
  readonly faceHasard = signal<number | string>('?');

  readonly aConditionHasard = computed(
    () =>
      this.liens().some((lien) => lien.conditions.some((condition) => condition.type === 'HASARD')) ||
      this.effets().some((effet) => effet.conditions.some((condition) => condition.type === 'HASARD')),
  );

  revelerHasard(): void {
    if (this.hasardRoule() || this.hasardResultatVisible() || this.hasardTermine()) {
      return;
    }

    const valeur = this.chapitre()?.tirageHasard;

    if (valeur === null || valeur === undefined) {
      return;
    }

    // 1. Le dé roule
    this.hasardRoule.set(true);

    setTimeout(() => {
      // 2. Le dé s'arrête sur la vraie valeur
      this.faceHasard.set(valeur);
      this.hasardRoule.set(false);
      this.hasardResultatVisible.set(true);

      // 3. On laisse le résultat affiché 2 secondes
      setTimeout(() => {
        this.hasardResultatVisible.set(false);
        this.hasardTermine.set(true);
        this.enregistrerChapitreHasardTermine();
      }, 2000);
    }, 640);
  }

  selectionnerOnglet(onglet: 'chapitre' | 'combat' | 'objets' | 'effets'): void {
    if (onglet === 'objets') {
      this.ongletObjetsClique.set(true);
      this.enregistrerChapitreObjetClique();
    }

    // Si on reclique sur l'onglet déjà ouvert, on ferme l'encart
    if (this.ongletActif() === onglet && onglet !== 'chapitre') {
      this.ongletActif.set('chapitre');
    } else {
      this.ongletActif.set(onglet);
    }

    this.ongletLeve.set(onglet);

    setTimeout(() => {
      this.ongletLeve.set(null);
    }, 300);
  }

  constructor() {
    // La feuille "SAC À DOS" (globale, montée à la racine) garde sa propre
    // copie de la fiche personnage : un ramassage/retrait fait depuis elle
    // ne met pas à jour automatiquement celle de cette page. On synchronise
    // ici dès que la feuille notifie une mise à jour pour CE personnage.
    effect(() => {
      const nouveau = this.inventaireSheet.personnageMisAJour();
      if (nouveau && nouveau.id === this.personnageId()) {
        this.personnage.set(nouveau);
      }
    });
  }

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
      error: (err) => console.error('Erreur lors du chargement du personnage :', err),
    });

    // 2. Récupération de toutes les disciplines disponibles (GET /disciplines)
    this.disciplineService.lister().subscribe({
      next: (disciplines) => {
        this.toutesDisciplines.set(disciplines);
      },
      error: (err) => console.error('Erreur lors du chargement des disciplines :', err),
    });

    // 3. Récupération de tous les objets disponibles (GET /objets)
    this.objetService.lister().subscribe({
      next: (objets) => {
        this.tousObjets.set(objets);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des objets :', err);
      },
    });

    // 4. Récupération du chapitre (GET /personnages/{id}/chapitre)
    this.chapitreService.getChapitreCourant(id).subscribe({
      next: (data) => {
        this.chapitre.set(data);

        this.ongletActif.set('chapitre');
        this.ongletLeve.set(null);
        this.ongletObjetsClique.set(this.chapitreObjetDejaClique(data.id));

        const hasardDejaTermine = this.chapitreHasardDejaTermine(data.id);
        this.hasardRoule.set(false);
        this.hasardResultatVisible.set(false);
        this.hasardTermine.set(hasardDejaTermine);
        this.faceHasard.set(hasardDejaTermine ? (data.tirageHasard ?? '?') : '?');

        this.chargement.set(false);
      },
      error: (err) => {
        console.error('Erreur lors de la récupération du chapitre :', err);
        this.erreur.set('Impossible de charger le chapitre en cours.');
        this.chargement.set(false);
      },
    });
  }

  nomDiscipline(id: string | null): string {
    if (!id) {
      return 'Discipline requise';
    }

    const discipline = this.toutesDisciplines().find(
      (d) => d.id.toLowerCase() === id.toLowerCase(),
    );

    return discipline?.nom ?? id;
  }

  private chapitreObjetDejaClique(chapitreId: number): boolean {
    return this.lireChapitreObjetClique() === chapitreId;
  }

  private enregistrerChapitreObjetClique(): void {
    const chapitreId = this.chapitre()?.id;
    if (chapitreId === undefined) {
      return;
    }

    localStorage.setItem(this.chapitreObjetCliqueKey, String(chapitreId));
  }

  private lireChapitreObjetClique(): number | null {
    const valeur = localStorage.getItem(this.chapitreObjetCliqueKey);
    const chapitreId = valeur === null ? NaN : Number(valeur);
    return Number.isFinite(chapitreId) ? chapitreId : null;
  }

  private chapitreHasardDejaTermine(chapitreId: number): boolean {
    return this.lireChapitreHasardTermine() === this.chapitreHasardTermineValeur(chapitreId);
  }

  private enregistrerChapitreHasardTermine(): void {
    const chapitreId = this.chapitre()?.id;
    if (chapitreId === undefined) {
      return;
    }

    localStorage.setItem(this.chapitreHasardTermineKey, this.chapitreHasardTermineValeur(chapitreId));
  }

  private lireChapitreHasardTermine(): string | null {
    return localStorage.getItem(this.chapitreHasardTermineKey);
  }

  // Inclut le personnageId : deux personnages passant par le même chapitre
  // (même id numérique) ne doivent pas partager cet état.
  private chapitreHasardTermineValeur(chapitreId: number): string {
    return `${this.personnageId()}:${chapitreId}`;
  }

  nomObjet(id: string | null): string {
    if (!id) {
      return 'Objet requis';
    }

    const objet = this.tousObjets().find((o) => o.id.toLowerCase() === id.toLowerCase());

    return objet?.nom ?? id;
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
        console.error("Erreur lors de l'avancement vers le chapitre :", err);
        this.erreur.set("Impossible d'avancer vers ce chapitre.");
        this.chargement.set(false);
      },
    });
  }

  /** Redirige vers la page d'accueil des personnages. */
  retourAccueil(): void {
    this.router.navigate(['/accueil']);
  }
}