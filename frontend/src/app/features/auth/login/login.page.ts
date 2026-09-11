import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  IonContent,
  IonHeader,
  IonTitle,
  IonToolbar,
  IonItem,
  IonLabel,
  IonInput,
  IonButton,
  IonNote,
} from '@ionic/angular';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonHeader,
    IonTitle,
    IonToolbar,
    IonItem,
    IonLabel,
    IonInput,
    IonButton,
    IonNote,
  ],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    identifiant: ['', Validators.required],
    password: ['', Validators.required],
  });

  soumettre(): void {
    if (this.form.invalid) {
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.enCours.set(false);
        this.router.navigateByUrl('/accueil');
      },
      error: () => {
        this.enCours.set(false);
        this.erreur.set('Identifiants invalides, ou compte non vérifié.');
      },
    });
  }
}
