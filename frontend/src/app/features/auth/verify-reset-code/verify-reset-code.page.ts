import {
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { Router } from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';

import {
  IonButton,
  IonContent,
  IonInput,
  IonNote,
} from '@ionic/angular';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-reset-code',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    IonContent,
    IonInput,
    IonButton,
    IonNote,
    TranslatePipe,
  ],
  templateUrl: './verify-reset-code.page.html',
  styleUrl: './verify-reset-code.page.scss',
})
export class VerifyResetCodePage implements OnInit {

  private readonly fb =
    inject(FormBuilder);

  private readonly authService =
    inject(AuthService);

  private readonly router =
    inject(Router);

  readonly email =
    signal('');

  readonly enCours =
    signal(false);

  readonly renvoiEnCours =
    signal(false);

  readonly erreur =
    signal<string | null>(null);

  readonly message =
    signal<string | null>(null);

  readonly form =
    this.fb.nonNullable.group({

      code: [
        '',
        [
          Validators.required,
          Validators.pattern(/^\d{6}$/),
        ],
      ],
    });

  ngOnInit(): void {

    const email =
      this.authService.getPasswordResetEmail();

    /*
     * Impossible d'arriver directement à cette
     * étape sans être passé par l'email.
     */
    if (!email) {

      this.router.navigateByUrl(
        '/auth/forgot-password',
      );

      return;
    }

    this.email.set(email);
  }

  soumettre(): void {

    if (
      this.form.invalid
      || this.enCours()
    ) {
      return;
    }

    const email = this.email();

    if (!email) {
      return;
    }

    const code =
      this.form.controls.code.value;

    this.enCours.set(true);
    this.erreur.set(null);
    this.message.set(null);

    this.authService
      .verifyResetCode(
        email,
        code,
      )
      .subscribe({

        next: () => {

          this.enCours.set(false);

          this.router.navigateByUrl(
            '/auth/reset-password',
          );
        },

        error: () => {

          this.enCours.set(false);

          this.erreur.set(
            'AUTH.VERIFY_RESET_CODE.ERREUR_CODE',
          );
        },
      });
  }

  renvoyerCode(): void {

    const email = this.email();

    if (
      !email
      || this.renvoiEnCours()
    ) {
      return;
    }

    this.renvoiEnCours.set(true);

    this.erreur.set(null);
    this.message.set(null);

    this.authService
      .forgotPassword(email)
      .subscribe({

        next: () => {

          this.renvoiEnCours.set(false);

          this.form.reset();

          this.message.set(
            'AUTH.VERIFY_RESET_CODE.CODE_RENVOYE',
          );
        },

        error: () => {

          this.renvoiEnCours.set(false);

          this.erreur.set(
            'AUTH.VERIFY_RESET_CODE.ERREUR_RENVOI',
          );
        },
      });
  }

  modifierEmail(): void {

    this.authService
      .clearPasswordResetFlow();

    this.router.navigateByUrl(
      '/auth/forgot-password',
    );
  }
}