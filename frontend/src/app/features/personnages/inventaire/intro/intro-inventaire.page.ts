import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-intro-inventaire',
    standalone: true,
    imports: [IonContent, TranslatePipe],
    templateUrl: './intro-inventaire.page.html',
    styleUrl: './intro-inventaire.page.scss'
})
export class IntroInventairePage {
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);

    private readonly personnageId = this.route.snapshot.paramMap.get('id')!;

    retour(): void {
        this.router.navigate(['/accueil']);
    }

    continuer(): void {
        this.router.navigate(['/personnages', this.personnageId, 'inventaire', 'depart']);
    }
}

    