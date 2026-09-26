import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  inject,
  signal,
} from '@angular/core';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import {
  Router,
  RouterLink,
} from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';

import {
  IonButton,
  IonContent,
  IonInput,
  IonNote,
} from '@ionic/angular';

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

  private readonly fb =
    inject(FormBuilder);

  private readonly authService =
    inject(AuthService);

  private readonly router =
    inject(Router);

  readonly enCours =
    signal(false);

  readonly erreur =
    signal<string | null>(null);

  readonly form =
    this.fb.nonNullable.group({

      email: [
        '',
        [
          Validators.required,
          Validators.email,
        ],
      ],
    });

  soumettre(): void {

    if (
      this.form.invalid
      || this.enCours()
    ) {
      return;
    }

    const email =
      this.form.controls.email.value;

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService
      .forgotPassword(email)
      .subscribe({

        next: () => {

          this.enCours.set(false);

          /*
           * NOUVELLE ETAPE :
           * on va d'abord demander le code.
           */
          this.router.navigateByUrl(
            '/auth/verify-reset-code',
          );
        },

        error: (err: HttpErrorResponse) => {

          this.enCours.set(false);

          this.erreur.set(
            err.status === 429 ? 'AUTH.ERREUR_TROP_DE_TENTATIVES' : 'AUTH.FORGOT_PASSWORD.ERREUR',
          );
        },
      });
  }
}