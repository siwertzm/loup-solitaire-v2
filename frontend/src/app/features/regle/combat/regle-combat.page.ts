import { Component, ElementRef, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent, NavController, ViewWillEnter } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import {
  BONUS_HABILITE_DEFENSE,
  COLONNE_EGALITE,
  COLONNES_QUOTIENT,
  DEGATS_INFLIGES,
  DEGATS_SUBIS,
  FATAL,
  ORDRE_CHIFFRES,
  REDUCTION_DEFENSE,
} from './table-combat.data';

/**
 * Dernière page de la chaîne "Règles du jeu" (intro -> disciplines ->
 * équipement -> combat), voir app.routes.ts. Documente le fonctionnement
 * réel du combat tel qu'implémenté (CombatPage/CombatService), y compris
 * les tables de dégâts (voir table-combat.data.ts), volontairement
 * différent du système à jets de dés sur papier du livre original.
 */
@Component({
  selector: 'app-regle-combat',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './regle-combat.page.html',
  styleUrl: './regle-combat.page.scss',
})
export class RegleCombatPage implements ViewWillEnter {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly navCtrl = inject(NavController);

  /**
   * Id du personnage quand cette page est ouverte depuis un combat (voir
   * CombatPage.ouvrirRegles : premier combat, ou bouton "?"). Null quand
   * on arrive par la chaîne "Règles du jeu" (intro -> ... -> combat).
   */
  readonly personnageCombatId = signal<string | null>(null);

  readonly FATAL = FATAL;
  readonly colonnes = COLONNES_QUOTIENT;
  readonly chiffres = ORDRE_CHIFFRES;
  readonly reductions = REDUCTION_DEFENSE;
  readonly bonusDefense = BONUS_HABILITE_DEFENSE;

  private readonly rangeeQuotients = viewChild<ElementRef<HTMLElement>>('rangeeQuotients');

  /** Colonne de quotient d'attaque sélectionnée dans la table des dégâts. */
  readonly colonne = signal(COLONNE_EGALITE);

  readonly lignes = computed(() => {
    const c = this.colonne();
    return this.chiffres.map((chiffre) => ({
      chiffre,
      ennemi: DEGATS_INFLIGES[c][chiffre],
      vous: DEGATS_SUBIS[c][chiffre],
    }));
  });

  constructor() {
    // Centre le quotient sélectionné par défaut ("0") dans la rangée défilante.
    afterNextRender(() => {
      const rangee = this.rangeeQuotients()?.nativeElement;
      const actif = rangee?.querySelector<HTMLElement>('.actif');
      if (rangee && actif) {
        const decalage = actif.getBoundingClientRect().left - rangee.getBoundingClientRect().left;
        rangee.scrollLeft += decalage - (rangee.clientWidth - actif.offsetWidth) / 2;
      }
    });
  }

  choisirColonne(index: number): void {
    this.colonne.set(index);
  }

  retour(): void {
    if (this.revenirAuCombat()) return;
    this.router.navigate(['/accueil']);
  }

  terminer(): void {
    if (this.revenirAuCombat()) return;
    this.router.navigate(['/accueil']);
  }

  /**
   * Relu à CHAQUE affichage, pas seulement à la construction : Ionic peut
   * réutiliser une instance de cette page déjà créée (ouverte avant depuis
   * le menu des règles), dont le constructeur ne repasserait pas.
   */
  ionViewWillEnter(): void {
    this.personnageCombatId.set(this.route.snapshot.queryParamMap.get('combat'));
  }

  /**
   * Ouverte depuis un combat : navigateBack ramène sur la CombatPage déjà
   * présente dans la pile Ionic (animation de retour), où
   * ionViewWillEnter relance le chargement et joue l'intro du combat.
   */
  private revenirAuCombat(): boolean {
    const id = this.personnageCombatId();
    if (!id) return false;
    this.navCtrl.navigateBack(['/personnages', id, 'combat']);
    return true;
  }

  back(): void {
    this.navCtrl.navigateBack(['/regle/equipement']);
  }
}