import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

interface ObjetPossible {
    cle: string;
    detail?: string;
    icone?: string;
    initiale: string;
}

const OBJETS_POSSIBLES: ObjetPossible[] = [
    { cle: 'INTRO_INVENTAIRE.OBJETS.GLAIVE', icone: 'assets/icon/epee.png', initiale: 'G' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.EPEE', icone: 'assets/icon/epee.png', initiale: 'É' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.CASQUE', icone: 'assets/icon/casque.png', initiale: 'C' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.REPAS', detail: '×2', icone: 'assets/icon/repas.png', initiale: 'R' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.COTTE_DE_MAILLES', icone: 'assets/icon/cotte_de_mailles.png', initiale: 'C' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.MASSE_D_ARMES', icone: 'assets/icon/masse.png', initiale: 'M' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.POTION_DE_SOIN', icone: 'assets/icon/potion.png', initiale: 'P' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.BATON', icone: 'assets/icon/baton.png', initiale: 'B' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.LANCE', icone: 'assets/icon/lance.png', initiale: 'L' },
    { cle: 'INTRO_INVENTAIRE.OBJETS.PIECES_D_OR', detail: '×12', icone: 'assets/icon/or.png', initiale: 'O' },
];

const ELEMENTS_BOURSE: ObjetPossible[] = [
    { cle: 'INTRO_INVENTAIRE.ELEMENTS_BOURSE.BOURSE', icone: 'assets/icon/bourse.png', initiale: 'B' },
    { cle: 'INTRO_INVENTAIRE.ELEMENTS_BOURSE.OR', icone: 'assets/icon/or.png', initiale: 'O' },
];

const EQUIPEMENT_DEPART: ObjetPossible[] = [
    { cle: 'INTRO_INVENTAIRE.EQUIPEMENT_DEPART.TUNIQUE', icone: 'assets/icon/tunique.png', initiale: 'T' },
    { cle: 'INTRO_INVENTAIRE.EQUIPEMENT_DEPART.CAPE', icone: 'assets/icon/cape.png', initiale: 'C' },
    { cle: 'INTRO_INVENTAIRE.EQUIPEMENT_DEPART.HACHE', icone: 'assets/icon/hache.png', initiale: 'H' },
    { cle: 'INTRO_INVENTAIRE.EQUIPEMENT_DEPART.REPAS', icone: 'assets/icon/repas.png', initiale: 'R' },
];

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

    readonly objetsPossibles = OBJETS_POSSIBLES;
    readonly elementsBourse = ELEMENTS_BOURSE;
    readonly equipementDepart = EQUIPEMENT_DEPART;

    retour(): void {
        this.router.navigate(['/accueil']);
    }

    continuer(): void {
        this.router.navigate(['/personnages', this.personnageId, 'inventaire', 'depart']);
    }
}

    