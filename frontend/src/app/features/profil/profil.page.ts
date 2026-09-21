import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IonContent, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { MoiResponse } from '../../core/models/personnage.model';
import { PersonnageService } from '../../core/services/personnage.service';
import { AuthService } from '../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './profil.page.html',
  styleUrl: './profil.page.scss',
})
export class ProfilPage implements ViewWillEnter {
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);
  private readonly auth = inject(AuthService);

  readonly compte = signal<MoiResponse | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  // Confirmation de suppression de compte : masquee par defaut, ouverte au
  // clic sur "Supprimer mon compte" (voir template). On ne monte pas de
  // ReactiveFormsModule pour un seul champ : on lit sa valeur via une
  // reference de template (#mdpSuppression) au moment de la confirmation.
  readonly suppressionOuverte = signal(false);
  readonly suppressionEnvoi = signal(false);
  readonly erreurSuppression = signal<string | null>(null);

  readonly initiale = computed(() => (this.compte()?.username ?? '?').charAt(0).toUpperCase());

  /**
   * "Membre depuis" formaté en français ("mars 2026"), à partir de
   * Utilisateur.dateCreation (backend). Repli pour les comptes créés
   * avant l'ajout de ce champ (dateCreation alors null côté backend).
   */
  readonly membreDepuis = computed(() => {
    const iso = this.compte()?.dateCreation;
    if (!iso) return 'PROFIL.MEMBRE_DEPUIS_INCONNU';
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(new Date(iso));
  });

  readonly lignes = computed(() => {
    const c = this.compte();
    if (!c) return [];
    return [
      { cle: 'PROFIL.LIGNE_PERSONNAGES', valeur: String(c.personnages?.length ?? 0), ton: 'clair' },
      { cle: 'PROFIL.LIGNE_EMAIL_VERIFIE', valeur: c.emailVerifie ? 'COMMUN.OUI' : 'COMMUN.NON', ton: c.emailVerifie ? 'vert' : 'rouge' },
      { cle: 'PROFIL.LIGNE_PIECES_PREMIUM', valeur: "999", ton: 'gold' },
      { cle: 'PROFIL.LIGNE_MEMBRE_DEPUIS', valeur: this.membreDepuis(), ton: 'clair' }
    ];
  });

  // ionViewWillEnter (et non ngOnInit) : ion-router-outlet garde cette page en
  // mémoire quand on va sur /profil/edition, le composant n'est pas recréé au
  // retour. Ce hook Ionic se redéclenche bien à chaque fois que la page
  // redevient active, contrairement à ngOnInit qui ne tourne qu'une fois.
  ionViewWillEnter(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.personnages$.moi().subscribe({
      next: (c) => {
        this.compte.set(c);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('PROFIL.ERREUR_CHARGEMENT');
        this.chargement.set(false);
      },
    });
  }

  retour(): void {
    this.router.navigate(['/accueil']);
  }

  modifierEmail(): void {
    this.router.navigate(['/profil/edition']);
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login'], { replaceUrl: true }),
      // Le refresh token était déjà invalide côté serveur : on nettoie quand
      // même localement pour ne pas bloquer l'utilisateur sur cet écran.
      error: () => {
        this.auth.clearSessionLocale();
        this.router.navigate(['/auth/login'], { replaceUrl: true });
      },
    });
  }

  ouvrirSuppression(): void {
    this.suppressionOuverte.set(true);
    this.erreurSuppression.set(null);
  }
 
  annulerSuppression(): void {
    this.suppressionOuverte.set(false);
    this.erreurSuppression.set(null);
  }
 
  confirmerSuppression(motDePasse: string): void {
    if (!motDePasse || this.suppressionEnvoi()) return;
    this.suppressionEnvoi.set(true);
    this.erreurSuppression.set(null);
    this.auth.supprimerCompte(motDePasse).subscribe({
      next: () => this.router.navigate(['/auth/login'], { replaceUrl: true }),
      error: (err: HttpErrorResponse) => {
        this.suppressionEnvoi.set(false);
        this.erreurSuppression.set(
          err.status === 401 ? 'PROFIL.ERREUR_SUPPRESSION_MDP' : 'PROFIL.ERREUR_SUPPRESSION',
        );
      },
    });
  }

  private static readonly URL_FEEDBACK = 'https://forms.gle/vgSVnrKR1t8aiucr9';

  ouvrirFormulaireAvis(): void {
    window.open(ProfilPage.URL_FEEDBACK, '_blank', 'noopener,noreferrer');
  }
}