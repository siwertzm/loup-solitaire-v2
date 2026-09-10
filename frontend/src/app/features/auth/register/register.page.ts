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
  selector: 'app-register',
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
  templateUrl: './register.page.html',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly succes = signal(false);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  soumettre(): void {
    if (this.form.invalid) {
      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.enCours.set(false);
        this.succes.set(true);
      },
      error: () => {
        this.enCours.set(false);
        this.erreur.set("Inscription impossible : nom d'utilisateur ou email déjà utilisé.");
      },
    });
  }

  allerVersLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}
