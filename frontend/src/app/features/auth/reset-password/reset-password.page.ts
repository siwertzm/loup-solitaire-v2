import { Component, inject, OnInit, signal } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';

import { IonButton, IonContent, IonInput, IonInputPasswordToggle, IonNote } from '@ionic/angular';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
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
  templateUrl: './reset-password.page.html',
  styleUrl: './reset-password.page.scss',
})
export class ResetPasswordPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly succes = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],

    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],

    password: ['', [Validators.required, Validators.minLength(8)]],

    confirmation: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    const email = this.route.snapshot.queryParamMap.get('email');

    if (email) {
      this.form.controls.email.setValue(email);
    }
  }

  soumettre(): void {
    if (this.form.invalid || this.enCours()) {
      return;
    }

    const { email, code, password, confirmation } = this.form.getRawValue();

    if (password !== confirmation) {
      this.erreur.set('AUTH.RESET_PASSWORD.ERREUR_PASSWORDS_DIFFERENTS');

      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService.resetPassword(email, code, password).subscribe({
      next: () => {
        this.enCours.set(false);
        this.succes.set(true);
      },

      error: () => {
        this.enCours.set(false);

        this.erreur.set('AUTH.RESET_PASSWORD.ERREUR_CODE');
      },
    });
  }

  allerVersLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}
