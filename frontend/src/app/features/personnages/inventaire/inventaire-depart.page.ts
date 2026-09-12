import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';

import { InventaireItem } from '../../../core/models/personnage.model';
import { PersonnageService } from '../../../core/services/personnage.service';

/**
 * Équipement de départ — écran de révélation, affiché juste après
 * POST /personnages.
 *
 * Les deux tirages (Pièces d'Or et objet de départ aléatoire) sont faits
 * par le backend dans PersonnageService.equiperMateriel() : cet écran ne
 * tire rien, il révèle le contenu réel de l'inventaire. Les dés sont
 * purement cosmétiques (même principe que la page de création), ils
 * s'arrêtent sur la valeur renvoyée par l'API.
 *
 * Équipement fixe côté backend : hache, repas ×1, carte, coin ×999.
 * Table 0-9 de l'objet aléatoire : glaive, épée, casque, repas ×2,
 * cotte de mailles, masse, potion de soin, bâton, lance, or ×12.
 */

interface Case {
  nom: string;
  note: string;
  remplie: boolean;
  trouvee: boolean;
}

const MAX_ARMES = 2;
const MAX_OBJETS_ET_REPAS = 8;
const NB_CASES_OBJETS = 4;
const NB_CASES_REPAS = 4;
const ID_OR = 'or';
const ID_CARTE = 'carte';
const ID_COIN = 'coin';
const ID_HACHE = 'hache';

/** Icônes des objets de la table de départ (clé = objetId du backend). */
const ICONES: Record<string, string[]> = {
  hache: ['M6 19l7-7', 'M13.5 4.5h6v5h-6z', 'M13.5 7h-2l-2 2 2.5 2.5 2-2z'],
  glaive: ['M12 21V10', 'M12 3l2.5 4.5L12 10 9.5 7.5 12 3z', 'M9.5 12h5'],
  epee: ['M18.5 3.5 9 13l2 2 9.5-9.5V3.5z', 'M9 13l-4.5 6.5 2 2L13 17'],
  casque: ['M5 13a7 7 0 0 1 14 0v4H5v-4z', 'M12 6V3', 'M5 17h14'],
  repas: ['M4 11h16', 'M5.5 11c0 4 2.9 7 6.5 7s6.5-3 6.5-7', 'M9 7.5c0-1.5 1.5-1.8 1.5-3.5M14 7.5c0-1.5 1.5-1.8 1.5-3.5'],
  cotte_de_mailles: ['M12 3l7 3v6c0 5-3.5 7.5-7 9-3.5-1.5-7-4-7-9V6l7-3z', 'M9 9h6M9 12.5h6M10.5 16h3'],
  masse: ['M6 20l6-6', 'M16 2v2M16 12v2M10 8h2M20 8h2'],
  potion_de_soin: ['M10 3h4v3.5l3 4.5v7a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2v-7l3-4.5V3z', 'M7.5 14h9'],
  baton: ['M6 20L18 5', 'M16 3.5h3.5V7'],
  lance: ['M12 21V9', 'M12 3l3 5H9l3-5z', 'M9.5 11.5h5'],
  or: ['M6 8v4c0 1.4 2.7 2.6 6 2.6s6-1.2 6-2.6V8', 'M6 12.5v3.5c0 1.4 2.7 2.6 6 2.6s6-1.2 6-2.6v-3.5'],
};

const ORDRE_TABLE = [
  'glaive',
  'epee',
  'casque',
  'repas',
  'cotte_de_mailles',
  'masse',
  'potion_de_soin',
  'baton',
  'lance',
  'or',
];

@Component({
  selector: 'app-inventaire-depart',
  standalone: true,
  imports: [IonContent, TranslatePipe],
  templateUrl: './inventaire-depart.page.html',
  styleUrl: './inventaire-depart.page.scss',
})
export class InventaireDepartPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly personnages$ = inject(PersonnageService);

  private readonly personnageId = this.route.snapshot.paramMap.get('id')!;

  readonly nom = signal('');
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);

  /** Contenu réel de l'inventaire renvoyé par l'API. */
  private readonly inventaire = signal<InventaireItem[]>([]);

  /** Pièces d'Or : null tant que le dé n'a pas été « lancé » (révélé). */
  readonly or = signal<number | null>(null);
  readonly faceOr = signal<number | string>('?');
  readonly roulantOr = signal(false);

  /** Objet de départ aléatoire : id révélé, et id affiché pendant l'animation. */
  readonly objet = signal<string | null>(null);
  readonly faceObjet = signal<string | null>(null);
  readonly roulantObjet = signal(false);

  readonly orRevele = computed(() => this.or() !== null && !this.roulantOr());
  readonly objetRevele = computed(() => this.objet() !== null && !this.roulantObjet());
  readonly complet = computed(() => this.orRevele() && this.objetRevele());

  /** Trait du dé d'objet : icône pendant/après le tirage, « ? » avant. */
  readonly traitsObjet = computed(() => {
    const id = this.roulantObjet() ? this.faceObjet() : this.objet();
    return id ? (ICONES[id] ?? []) : [];
  });

  private objetItem(): InventaireItem | null {
    // L'objet de départ aléatoire est le seul item hors équipement fixe.
    return (
      this.inventaire().find(
        (i) => ![ID_HACHE, ID_CARTE, ID_COIN, ID_OR].includes(i.objetId) && !(i.objetId === 'repas' && i.quantite === 1),
      ) ?? null
    );
  }

  readonly nomObjet = computed(() => this.objetItem()?.nom ?? '');

  readonly casesArmes = computed<Case[]>(() => {
    const armes = this.inventaire().filter((i) => i.categorie === 'ARME');
    const cases = armes
      .slice(0, MAX_ARMES)
      .map<Case>((a) => ({ nom: a.nom, note: '', remplie: true, trouvee: a.objetId !== ID_HACHE }));
    while (cases.length < MAX_ARMES) cases.push({ nom: '', note: '', remplie: false, trouvee: false });
    return this.objetRevele() ? cases : cases.map((c, i) => (i === 0 ? c : { nom: '', note: '', remplie: false, trouvee: false }));
  });

  readonly casesObjets = computed<Case[]>(() => this.casesSac('OBJET', NB_CASES_OBJETS));
  readonly casesRepas = computed<Case[]>(() => this.casesSac('REPAS', NB_CASES_REPAS));

  readonly casesSpeciaux = computed(() =>
    this.inventaire()
      .filter((i) => i.categorie === 'OBJETS_SPECIAUX')
      .filter((i) => this.objetRevele() || i.objetId === ID_CARTE || i.objetId === ID_COIN)
      .map((i) => ({ nom: i.nom, note: i.quantite > 1 ? `×${i.quantite}` : '' })),
  );

  readonly sacCompte = computed(
    () =>
      this.inventaire()
        .filter((i) => i.categorie === 'OBJET' || i.categorie === 'REPAS')
        .reduce((total, i) => total + i.quantite, 0),
  );

  readonly armesCompte = computed(() => this.casesArmes().filter((c) => c.remplie).length);

  readonly maxSac = MAX_OBJETS_ET_REPAS;
  readonly maxArmes = MAX_ARMES;

  ngOnInit(): void {
    this.personnages$.recuperer(this.personnageId).subscribe({
      next: (p) => {
        this.nom.set(p.nom);
        this.inventaire.set(p.inventaire ?? []);
        this.chargement.set(false);
      },
      error: () => {
        this.erreur.set('EQUIPEMENT.ERREUR_CHARGEMENT');
        this.chargement.set(false);
      },
    });
  }

  /** Révèle les Pièces d'Or tirées par le backend (animation seule). */
  revelerOr(): void {
    if (this.roulantOr() || this.or() !== null) return;
    const bourse = this.inventaire().find((i) => i.objetId === ID_OR);
    const valeur = bourse ? bourse.quantite : 0;
    this.roulantOr.set(true);
    setTimeout(() => {
      this.faceOr.set(valeur);
      this.or.set(valeur);
      this.roulantOr.set(false);
    }, 640);
  }

  /** Révèle l'objet de départ tiré par le backend (défilé d'icônes puis arrêt). */
  revelerObjet(): void {
    if (this.roulantObjet() || this.objet() !== null) return;
    const item = this.objetItem();
    if (!item) return;
    this.roulantObjet.set(true);
    ORDRE_TABLE.forEach((id, i) => setTimeout(() => this.faceObjet.set(id), 100 + i * 55));
    setTimeout(() => {
      this.faceObjet.set(item.objetId);
      this.objet.set(item.objetId);
      this.roulantObjet.set(false);
    }, 700);
  }

  partir(): void {
    if (!this.complet()) return;
    this.router.navigate(['/personnages', this.personnageId, 'chapitre'], { replaceUrl: true });
  }

  private casesSac(categorie: 'OBJET' | 'REPAS', nbCases: number): Case[] {
    const cases: Case[] = [];
    this.inventaire()
      .filter((i) => i.categorie === categorie)
      .forEach((i) => {
        const trouvee = i.objetId === this.objet();
        // Le repas fixe (×1) est visible d'entrée, les exemplaires issus du
        // tirage n'apparaissent qu'une fois l'objet révélé.
        const quantite = trouvee && !this.objetRevele() ? 0 : i.quantite;
        for (let n = 0; n < quantite; n++) {
          cases.push({ nom: i.nom, note: '', remplie: true, trouvee: trouvee && n > 0 });
        }
      });
    while (cases.length < nbCases) cases.push({ nom: '', note: '', remplie: false, trouvee: false });
    return cases.slice(0, nbCases);
  }
}
