import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { PersonnageService } from '../../../core/services/personnage.service';

@Component({
  selector: 'app-profil-edition',
  standalone: true,
  imports: [IonContent, ReactiveFormsModule],
  templateUrl: './profil-edition.page.html',
  styleUrl: './profil-edition.page.scss',
})
export class ProfilEditionPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);

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
        this.erreur.set('Impossible de charger ton profil.');
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
    // TODO backend : PATCH /auth/me { username, email }
    this.personnages$.majCompte(this.compteForm.getRawValue()).subscribe({
      next: (c) => {
        this.envoi.set(false);
        this.emailVerifie.set(c.emailVerifie);
        this.compteForm.patchValue({ username: c.username, email: c.email });
        this.message.set(
          c.emailVerifie ? 'Profil mis à jour.' : 'Profil mis à jour. Vérifie ta nouvelle adresse email.',
        );
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('La mise à jour a échoué.');
      },
    });
  }

  changerMotDePasse(): void {
    const v = this.motDePasseForm.getRawValue();
    if (this.motDePasseForm.invalid || this.envoi()) return;
    if (v.nouveau !== v.confirmation) {
      this.erreur.set('Les deux mots de passe ne correspondent pas.');
      return;
    }
    this.envoi.set(true);
    this.erreur.set(null);
    // TODO backend : POST /auth/mot-de-passe { actuel, nouveau }
    this.personnages$.changerMotDePasse(v.actuel, v.nouveau).subscribe({
      next: () => {
        this.envoi.set(false);
        this.motDePasseForm.reset();
        this.message.set('Mot de passe modifié.');
      },
      error: () => {
        this.envoi.set(false);
        this.erreur.set('Mot de passe actuel incorrect.');
      },
    });
  }

  renvoyerVerification(): void {
    this.personnages$.renvoyerVerification().subscribe({
      next: () => this.message.set('Email de vérification renvoyé.'),
      error: () => this.erreur.set("L'envoi a échoué."),
    });
  }
}
