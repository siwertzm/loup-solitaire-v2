import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';

import { PersonnageService } from '../../../core/services/personnage.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profil-edition',
  standalone: true,
  imports: [IonContent, ReactiveFormsModule, TranslatePipe],
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

  readonly compteForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
    email: ['', [Validators.required, Validators.email]],
  });

  readonly motDePasseForm = this.fb.nonNullable.group({
    actuel: ['', Validators.required],
    nouveau: ['', [Validators.required, Validators.minLength(8)]],
    confirmation: ['', Validators.required],
  });

  ngOnInit(): void {
    this.personnages$.moi().subscribe({
      next: (c) => {
        this.compteForm.patchValue({ username: c.username, email: c.email });
        this.emailVerifie.set(c.emailVerifie);
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
    this.envoi.set(true);
    this.erreur.set(null);
    this.personnages$.majCompte(this.compteForm.getRawValue()).subscribe({
      next: (c) => {
        this.envoi.set(false);
        this.emailVerifie.set(c.emailVerifie);
        this.compteForm.patchValue({ username: c.username, email: c.email });
        this.message.set(
          c.emailVerifie ? 'PROFIL_EDITION.SUCCES_PROFIL_MAJ' : 'PROFIL_EDITION.SUCCES_PROFIL_MAJ_EMAIL',
        );
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('PROFIL_EDITION.ERREUR_MAJ');
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
    const email = this.compteForm.controls.email.value;
    this.personnages$.renvoyerVerification(email).subscribe({
      next: () => this.message.set('PROFIL_EDITION.SUCCES_VERIFICATION'),
      error: () => this.erreur.set('PROFIL_EDITION.ERREUR_VERIFICATION'),
    });
  }
}