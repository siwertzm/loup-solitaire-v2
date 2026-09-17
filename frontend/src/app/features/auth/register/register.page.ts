import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import {
  IonContent,
  IonInput,
  IonInputPasswordToggle,
  IonButton,
  IonNote,
} from '@ionic/angular';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonInput,
    IonInputPasswordToggle,
    IonButton,
    IonNote,
    TranslatePipe,
  ],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly succes = signal(false);

  readonly form = this.fb.nonNullable.group({
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
      ],
    ],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],

    // Vérification uniquement côté frontend.
    confirmation: ['', [Validators.required, Validators.minLength(8)]],
  });

  soumettre(): void {
    if (this.form.invalid || this.enCours()) {
      return;
    }

    const {
      username,
      email,
      password,
      confirmation,
    } = this.form.getRawValue();

    // Même contrôle que dans l'édition du profil.
    if (password !== confirmation) {
      this.erreur.set('AUTH.REGISTER.ERREUR_PASSWORDS_DIFFERENTS');
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    // On n'envoie PAS "confirmation" au backend.
    this.authService.register({
      username,
      email,
      password,
    }).subscribe({
      next: () => {
        this.enCours.set(false);
        this.succes.set(true);
      },
      error: () => {
        this.enCours.set(false);
        this.erreur.set('AUTH.REGISTER.ERREUR_INSCRIPTION');
      },
    });
  }

  allerVersLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}