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

import {
  Router,
  RouterLink,
} from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';

import {
  IonButton,
  IonContent,
  IonInput,
  IonInputPasswordToggle,
  IonNote,
} from '@ionic/angular';

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

  private readonly fb =
    inject(FormBuilder);

  private readonly authService =
    inject(AuthService);

  private readonly router =
    inject(Router);

  private resetToken: string | null = null;

  readonly enCours =
    signal(false);

  readonly erreur =
    signal<string | null>(null);

  readonly succes =
    signal(false);

  readonly form =
    this.fb.nonNullable.group({

      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
        ],
      ],

      confirmation: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
        ],
      ],
    });

  ngOnInit(): void {

    this.resetToken =
      this.authService
        .getPasswordResetToken();

    /*
     * L'utilisateur ne peut pas ouvrir directement
     * cette page sans avoir validé le code.
     */
    if (!this.resetToken) {

      const email =
        this.authService
          .getPasswordResetEmail();

      this.router.navigateByUrl(
        email
          ? '/auth/verify-reset-code'
          : '/auth/forgot-password',
      );
    }
  }

  soumettre(): void {

    if (
      this.form.invalid
      || this.enCours()
      || !this.resetToken
    ) {
      return;
    }

    const {
      password,
      confirmation,
    } = this.form.getRawValue();

    if (
      password !== confirmation
    ) {

      this.erreur.set(
        'AUTH.RESET_PASSWORD.ERREUR_PASSWORDS_DIFFERENTS',
      );

      return;
    }

    this.enCours.set(true);
    this.erreur.set(null);

    this.authService
      .resetPassword(
        this.resetToken,
        password,
      )
      .subscribe({

        next: () => {

          this.enCours.set(false);

          this.succes.set(true);

          /*
           * Le service vient d'effacer
           * email + resetToken du sessionStorage.
           */
          this.resetToken = null;
        },

        error: () => {

          this.enCours.set(false);

          this.erreur.set(
            'AUTH.RESET_PASSWORD.ERREUR_TOKEN',
          );
        },
      });
  }

  allerVersLogin(): void {

    this.router.navigateByUrl(
      '/auth/login',
    );
  }
}