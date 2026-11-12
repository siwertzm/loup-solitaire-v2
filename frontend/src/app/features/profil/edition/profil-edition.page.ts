import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent, IonIcon } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { eyeOffOutline, eyeOutline } from 'ionicons/icons';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';

import { MoiResponse } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';
import { AuthService } from '../../../core/services/auth.service';

addIcons({ 'eye-outline': eyeOutline, 'eye-off-outline': eyeOffOutline });

@Component({
  selector: 'app-profil-edition',
  standalone: true,
  imports: [IonContent, IonIcon, ReactiveFormsModule, TranslatePipe],
  templateUrl: './profil-edition.page.html',
  styleUrl: './profil-edition.page.scss',
})
export class ProfilEditionPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);
  private readonly auth = inject(AuthService);

  readonly chargement = signal(true);
  readonly envoi = signal(false);
  readonly message = signal<string | null>(null);
  readonly erreur = signal<string | null>(null);
  readonly emailVerifie = signal(false);
  /** E-mail du compte (celui qui sert à se connecter). */
  readonly emailCompte = signal('');
  /** SEC-06 : nouvel e-mail en attente de confirmation. */
  readonly emailEnAttente = signal<string | null>(null);

  /** Champs mot de passe actuellement affichés en clair. */
  private readonly mdpAffiches = signal<ReadonlySet<string>>(new Set());

  mdpVisible(champ: string): boolean {
    return this.mdpAffiches().has(champ);
  }

  basculerMdp(champ: string): void {
    this.mdpAffiches.update((actuels) => {
      const suivants = new Set(actuels);
      if (suivants.has(champ)) {
        suivants.delete(champ);
      } else {
        suivants.add(champ);
      }
      return suivants;
    });
  }

  readonly compteForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30), Validators.pattern(/^[^@]*$/)]],
    email: ['', [Validators.required, Validators.email]],
    // SEC-06 : exigé seulement si l'e-mail change (voir emailModifie).
    motDePasseActuel: [''],
  });

  /** L'e-mail saisi diffère de celui du compte : le mot de passe est alors exigé. */
  emailModifie(): boolean {
    return this.compteForm.controls.email.value.trim().toLowerCase() !== this.emailCompte();
  }

  private appliquerCompte(c: MoiResponse): void {
    this.emailVerifie.set(c.emailVerifie);
    this.emailCompte.set(c.email);
    this.emailEnAttente.set(c.emailEnAttente);
    this.compteForm.patchValue({ username: c.username, email: c.email, motDePasseActuel: '' });
  }

  readonly motDePasseForm = this.fb.nonNullable.group({
    actuel: ['', Validators.required],
    nouveau: ['', [Validators.required, Validators.minLength(8)]],
    confirmation: ['', Validators.required],
  });

  ngOnInit(): void {
    this.personnages$.moi().subscribe({
      next: (c) => {
        this.appliquerCompte(c);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('PROFIL_EDITION.ERREUR_CHARGEMENT');
        this.chargement.set(false);
      },
    });
  }

  retour(): void {
    this.router.navigate(['/profil']);
  }

  enregistrerCompte(): void {
    if (this.compteForm.invalid || this.envoi()) return;
    const { username, email, motDePasseActuel } = this.compteForm.getRawValue();
    const changementEmail = this.emailModifie();
    if (changementEmail && !motDePasseActuel) {
      this.erreur.set('PROFIL_EDITION.ERREUR_MDP_REQUIS_EMAIL');
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);
    this.message.set(null);
    this.personnages$
      .majCompte(changementEmail ? { username, email, motDePasseActuel } : { username })
      .subscribe({
        next: (c) => {
          this.envoi.set(false);
          this.appliquerCompte(c);
          this.message.set(
            changementEmail ? 'PROFIL_EDITION.SUCCES_EMAIL_EN_ATTENTE' : 'PROFIL_EDITION.SUCCES_PROFIL_MAJ',
          );
        },
        error: (err: HttpErrorResponse) => {
          this.envoi.set(false);
          this.erreur.set(
            err.status === 401
              ? 'PROFIL_EDITION.ERREUR_MDP_ACTUEL'
              : err.status === 409
                ? 'PROFIL_EDITION.ERREUR_DEJA_UTILISE'
                : 'PROFIL_EDITION.ERREUR_MAJ',
          );
        },
      });
  }

  changerMotDePasse(): void {
    const v = this.motDePasseForm.getRawValue();
    if (this.motDePasseForm.invalid || this.envoi()) return;
    if (v.nouveau !== v.confirmation) {
      this.erreur.set('PROFIL_EDITION.ERREUR_MDP_DIFFERENTS');
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);
    this.auth.changePassword(v.actuel, v.nouveau).subscribe({
      next: () => {
        this.envoi.set(false);
        this.motDePasseForm.reset();
        this.message.set('PROFIL_EDITION.SUCCES_MDP');
      },
      error: (err: HttpErrorResponse) => {
        this.envoi.set(false);
        this.erreur.set(
          err.status === 401 ? 'PROFIL_EDITION.ERREUR_MDP_ACTUEL' : 'PROFIL_EDITION.ERREUR_MDP_MODIFICATION',
        );
      },
    });
  }

  renvoyerVerification(): void {
    // Toujours l'e-mail du compte : le serveur envoie le lien à l'adresse en attente s'il y en a une.
    this.personnages$.renvoyerVerification(this.emailCompte()).subscribe({
      next: () => this.message.set('PROFIL_EDITION.SUCCES_VERIFICATION'),
      error: (err: HttpErrorResponse) =>
        this.erreur.set(err.status === 429 ? 'AUTH.ERREUR_TROP_DE_TENTATIVES' : 'PROFIL_EDITION.ERREUR_VERIFICATION'),
    });
  }
}