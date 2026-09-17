import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { IonButton, IonContent, IonInput, IonNote } from '@ionic/angular';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonContent,
    IonInput,
    IonButton,
    IonNote,
    TranslatePipe,
  ],
  templateUrl: './forgot-password.page.html',
  styleUrl: './forgot-password.page.scss',
})
export class ForgotPasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  soumettre(): void {
    if (this.form.invalid || this.enCours()) {
      return;
    }

    const email = this.form.controls.email.value;

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.enCours.set(false);

        this.router.navigate(['/auth/reset-password'], {
          queryParams: {
            email,
          },
        });
      },

      error: () => {
        this.enCours.set(false);

        this.erreur.set('AUTH.FORGOT_PASSWORD.ERREUR');
      },
    });
  }
}
